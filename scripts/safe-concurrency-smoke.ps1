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

if (-not $SkipPreflight) {
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
    }
    else {
        & $preflightScript -EnvFile $envFile
    }

    if (-not $?) {
        throw "[safe-smoke] preflight config check failed."
    }
}

Write-Host "[safe-smoke] Applying conservative runtime limits..."
$env:PROCESSOR_RABBITMQ_CONCURRENCY = "1"
$env:PROCESSOR_RABBITMQ_MAX_CONCURRENCY = "2"
$env:PROCESSOR_RABBITMQ_PREFETCH = "2"
$env:PROCESSING_API_CONCURRENCY_LIMIT = "3"
$env:CRAWLER_MAX_CONCURRENT_SOURCES = "4"
$env:MAVEN_OPTS = "-Xmx$($MavenHeapMb)m"

if ($ReportFile -eq "") {
    $ReportFile = Join-Path $PSScriptRoot "safe-smoke-report.txt"
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
$samples = New-Object System.Collections.Generic.List[string]
$breachCount = 0
while (-not $process.HasExited) {
    Start-Sleep -Seconds $SampleIntervalSeconds

    $cpu = [math]::Round((Get-Counter '\Processor(_Total)\% Processor Time').CounterSamples[0].CookedValue, 2)
    $mem = [math]::Round((Get-Counter '\Memory\% Committed Bytes In Use').CounterSamples[0].CookedValue, 2)
    $sampleCount++
    if ($cpu -gt $peakCpu) { $peakCpu = $cpu }
    if ($mem -gt $peakMem) { $peakMem = $mem }
    $samples.Add(("{0}|cpu={1}|mem={2}" -f (Get-Date).ToString("s"), $cpu, $mem)) | Out-Null

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

$report = @(
    "status=" + ($(if ($exitCode -eq 0) { "passed" } else { "failed" })),
    ("checked_at=" + (Get-Date).ToString("s")),
    ("module=" + $Module),
    ("test_pattern=" + $TestPattern),
    ("also_make=" + (-not $DisableAlsoMake)),
    ("maven_heap_mb=" + $MavenHeapMb),
    ("cpu_threshold_percent=" + $CpuThresholdPercent),
    ("memory_threshold_percent=" + $MemoryThresholdPercent),
    ("sample_interval_seconds=" + $SampleIntervalSeconds),
    ("consecutive_breaches_to_stop=" + $ConsecutiveBreachesToStop),
    ("samples=" + $sampleCount),
    ("peak_cpu_percent=" + $peakCpu),
    ("peak_memory_percent=" + $peakMem),
    ("exit_code=" + $exitCode),
    "sample_log_begin"
)
$report += $samples
$report += "sample_log_end"
Set-Content -Path $ReportFile -Value $report -Encoding ascii
Write-Host ("[safe-smoke] Report written to {0}" -f $ReportFile)

if ($exitCode -ne 0) {
    throw "[safe-smoke] Maven exited with code $exitCode"
}

Write-Host "[safe-smoke] Completed successfully."
