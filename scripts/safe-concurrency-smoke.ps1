param(
    [string]$Module = "javainfohunter-processor",
    [string]$TestPattern = "ContentRoutingServiceImplTest",
    [int]$CpuThresholdPercent = 85,
    [int]$MemoryThresholdPercent = 85,
    [int]$SampleIntervalSeconds = 3,
    [int]$ConsecutiveBreachesToStop = 3
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "[safe-smoke] Applying conservative runtime limits..."
$env:PROCESSOR_RABBITMQ_CONCURRENCY = "1"
$env:PROCESSOR_RABBITMQ_MAX_CONCURRENCY = "2"
$env:PROCESSOR_RABBITMQ_PREFETCH = "2"
$env:PROCESSING_API_CONCURRENCY_LIMIT = "3"
$env:CRAWLER_MAX_CONCURRENT_SOURCES = "4"

$args = @(
    "-pl", $Module,
    "-am",
    "-Psafe-test",
    "-Dtest=$TestPattern",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "test"
)

$mavenCommand = "mvn.cmd $($args -join ' ')"
Write-Host "[safe-smoke] Starting: $mavenCommand"

# Run Maven through cmd.exe and use ProcessStartInfo for reliable exit code access.
$processInfo = New-Object System.Diagnostics.ProcessStartInfo
$processInfo.FileName = "cmd.exe"
$processInfo.Arguments = "/d /c `"$mavenCommand`""
$processInfo.UseShellExecute = $false
$process = New-Object System.Diagnostics.Process
$process.StartInfo = $processInfo
$null = $process.Start()

$breachCount = 0
while (-not $process.HasExited) {
    Start-Sleep -Seconds $SampleIntervalSeconds

    $cpu = [math]::Round((Get-Counter '\Processor(_Total)\% Processor Time').CounterSamples[0].CookedValue, 2)
    $mem = [math]::Round((Get-Counter '\Memory\% Committed Bytes In Use').CounterSamples[0].CookedValue, 2)

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

if ($exitCode -ne 0) {
    throw "[safe-smoke] Maven exited with code $exitCode"
}

Write-Host "[safe-smoke] Completed successfully."
