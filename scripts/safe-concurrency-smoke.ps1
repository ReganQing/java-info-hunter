param(
    [string]$Module = "javainfohunter-processor",
    [string]$TestPattern = "ContentRoutingServiceImplTest",
    [switch]$DisableAlsoMake,
    [switch]$SkipPreflight,
    [switch]$AllowPlaceholderSecrets,
    [int]$CpuThresholdPercent = 75,
    [int]$MemoryThresholdPercent = 80,
    [int]$SampleIntervalSeconds = 3,
    [int]$ConsecutiveBreachesToStop = 3,
    [int]$MavenHeapMb = 1024,
    [string]$ReportFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "report-common.ps1")

$report = [ordered]@{
    schemaVersion = "a5-5.v1"
    reportType = "safe-smoke"
    status = "running"
    checkedAt = (Get-Date).ToString("s")
    completedAt = $null
    module = $Module
    testPattern = $TestPattern
    alsoMake = (-not $DisableAlsoMake)
    allowPlaceholderSecrets = $AllowPlaceholderSecrets.IsPresent
    thresholds = [ordered]@{
        cpuPercent = $CpuThresholdPercent
        memoryPercent = $MemoryThresholdPercent
        sampleIntervalSeconds = $SampleIntervalSeconds
        consecutiveBreachesToStop = $ConsecutiveBreachesToStop
        mavenHeapMb = $MavenHeapMb
    }
    runtimeLimits = [ordered]@{
        processorRabbitmqConcurrency = 1
        processorRabbitmqMaxConcurrency = 2
        processorRabbitmqPrefetch = 2
        processingApiConcurrencyLimit = 3
        crawlerMaxConcurrentSources = 4
    }
    preflight = [ordered]@{
        status = "pending"
    }
    execution = [ordered]@{
        command = $null
        exitCode = $null
    }
    metrics = [ordered]@{
        sampleCount = 0
        peakCpuPercent = 0.0
        peakMemoryPercent = 0.0
        samples = @()
    }
    error = $null
}

Write-Host "[safe-smoke] Applying conservative runtime limits..."
$env:PROCESSOR_RABBITMQ_CONCURRENCY = "1"
$env:PROCESSOR_RABBITMQ_MAX_CONCURRENCY = "2"
$env:PROCESSOR_RABBITMQ_PREFETCH = "2"
$env:PROCESSING_API_CONCURRENCY_LIMIT = "3"
$env:CRAWLER_MAX_CONCURRENT_SOURCES = "4"
$env:MAVEN_OPTS = "-Xmx$($MavenHeapMb)m"

if ($ReportFile -eq "") {
    $ReportFile = Join-Path $PSScriptRoot "safe-smoke-report.json"
}

try {
    if ($SkipPreflight) {
        $report.preflight.status = "skipped"
    } else {
        $preflightScript = Join-Path $PSScriptRoot "validate-runtime-config.ps1"
        if (-not (Test-Path $preflightScript)) {
            throw "[safe-smoke] preflight script not found: $preflightScript"
        }

        $envFile = ".env"
        $allowPlaceholdersFromExample = $false
        if (-not (Test-Path $envFile)) {
            $envFile = ".env.example"
            $allowPlaceholdersFromExample = $true
        }

        Write-Host "[safe-smoke] Running preflight config check with $envFile ..."
        if ($AllowPlaceholderSecrets -or $allowPlaceholdersFromExample -or $envFile -eq ".env.example") {
            & $preflightScript -EnvFile $envFile -AllowPlaceholderSecrets
        } else {
            & $preflightScript -EnvFile $envFile
        }

        if (-not $?) {
            throw "[safe-smoke] preflight config check failed."
        }
        $report.preflight.status = "passed"
    }

    $args = @(
        "-T", "1",
        "-pl", $Module,
        "-Psafe-test",
        "-Dtest=$TestPattern",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "test"
    )

    if (-not $DisableAlsoMake) {
        $args = @("-T", "1", "-pl", $Module, "-am", "-Psafe-test", "-Dtest=$TestPattern", "-Dsurefire.failIfNoSpecifiedTests=false", "test")
    }

    $mavenCommand = "mvn.cmd $($args -join ' ')"
    $report.execution.command = $mavenCommand
    Write-Host "[safe-smoke] Starting: $mavenCommand"
    Write-Host ("[safe-smoke] MAVEN_OPTS={0}" -f $env:MAVEN_OPTS)

    # Run Maven through cmd.exe and use ProcessStartInfo for reliable exit code access.
    $processInfo = New-Object System.Diagnostics.ProcessStartInfo
    $processInfo.FileName = "cmd.exe"
    $processInfo.Arguments = "/d /c `"$mavenCommand`""
    $processInfo.UseShellExecute = $false
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $processInfo
    $null = $process.Start()

    $peakCpu = 0.0
    $peakMem = 0.0
    $sampleCount = 0
    $samples = New-Object System.Collections.Generic.List[object]
    $breachCount = 0
    while (-not $process.HasExited) {
        Start-Sleep -Seconds $SampleIntervalSeconds

        $cpu = [math]::Round((Get-Counter '\Processor(_Total)\% Processor Time').CounterSamples[0].CookedValue, 2)
        $mem = [math]::Round((Get-Counter '\Memory\% Committed Bytes In Use').CounterSamples[0].CookedValue, 2)
        $sampleCount++
        if ($cpu -gt $peakCpu) { $peakCpu = $cpu }
        if ($mem -gt $peakMem) { $peakMem = $mem }
        $samples.Add([ordered]@{
            timestamp = (Get-Date).ToString("s")
            cpuPercent = $cpu
            memoryPercent = $mem
        }) | Out-Null

        Write-Host ("[safe-smoke] host cpu={0}% mem={1}%" -f $cpu, $mem)

        if ($cpu -ge $CpuThresholdPercent -or $mem -ge $MemoryThresholdPercent) {
            $breachCount++
            Write-Warning ("[safe-smoke] threshold breach {0}/{1}" -f $breachCount, $ConsecutiveBreachesToStop)
        } else {
            $breachCount = 0
        }

        if ($breachCount -ge $ConsecutiveBreachesToStop) {
            Write-Error ("[safe-smoke] stopping build due to sustained high usage (cpu={0}%, mem={1}%)" -f $cpu, $mem)
            if (-not $process.HasExited) {
                Stop-Process -Id $process.Id -Force
            }
            throw "Aborted due to resource pressure"
        }
    }

    $process.WaitForExit()
    $exitCode = $process.ExitCode
    if ($null -eq $exitCode) {
        throw "[safe-smoke] Maven exit code is unavailable."
    }

    $report.execution.exitCode = $exitCode
    $report.metrics.sampleCount = $sampleCount
    $report.metrics.peakCpuPercent = $peakCpu
    $report.metrics.peakMemoryPercent = $peakMem
    $report.metrics.samples = @($samples.ToArray())

    if ($exitCode -ne 0) {
        $report.status = "failed"
        throw "[safe-smoke] Maven exited with code $exitCode"
    }

    $report.status = "passed"
    Write-Host "[safe-smoke] Completed successfully."
} catch {
    if ($report.status -eq "running") {
        $report.status = "failed"
    }
    if ($report.preflight.status -eq "pending") {
        $report.preflight.status = "failed"
    }
    $report.error = [ordered]@{
        message = $_.Exception.Message
        type = $_.Exception.GetType().FullName
    }
    throw
} finally {
    $report.completedAt = (Get-Date).ToString("s")
    Write-JsonReport -Path $ReportFile -Report $report
    Write-Host ("[safe-smoke] Report written to {0}" -f $ReportFile)
}
