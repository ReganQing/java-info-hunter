Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$services = @(
    @{ Name = "api"; Pattern = "ApiApplication" },
    @{ Name = "crawler"; Pattern = "CrawlerApplication" },
    @{ Name = "processor"; Pattern = "ProcessorApplication" }
)

foreach ($svc in $services) {
    $pidFile = Join-Path $scriptDir ($svc.Name + ".pid")
    if (-not (Test-Path $pidFile)) {
        $fallbackNoPid = Get-CimInstance Win32_Process | Where-Object {
            $_.CommandLine -and $_.CommandLine -like ("*" + $svc.Pattern + "*")
        }
        if ($fallbackNoPid) {
            foreach ($fp in $fallbackNoPid) {
                Stop-Process -Id $fp.ProcessId -Force -ErrorAction SilentlyContinue
                Write-Host ("[stop-all] Fallback stopped {0} PID={1}" -f $svc.Name, $fp.ProcessId)
            }
        } else {
            Write-Host ("[stop-all] {0} pid file not found." -f $svc.Name)
        }
        continue
    }

    $pidText = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    if (-not $pidText) {
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        continue
    }

    $targetPid = [int]$pidText
    $proc = Get-CimInstance Win32_Process -Filter ("ProcessId=" + $targetPid) -ErrorAction SilentlyContinue

    if ($proc -and $proc.CommandLine -and $proc.CommandLine -like ("*" + $svc.Pattern + "*")) {
        Stop-Process -Id $targetPid -Force -ErrorAction SilentlyContinue
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        Write-Host ("[stop-all] Stopped {0} PID={1}" -f $svc.Name, $targetPid)
        continue
    }

    $fallback = Get-CimInstance Win32_Process | Where-Object {
        $_.CommandLine -and $_.CommandLine -like ("*" + $svc.Pattern + "*")
    }

    if ($fallback) {
        foreach ($fp in $fallback) {
            Stop-Process -Id $fp.ProcessId -Force -ErrorAction SilentlyContinue
            Write-Host ("[stop-all] Fallback stopped {0} PID={1}" -f $svc.Name, $fp.ProcessId)
        }
    } else {
        Write-Host ("[stop-all] {0} pid not running: {1}" -f $svc.Name, $targetPid)
    }

    Remove-Item $pidFile -ErrorAction SilentlyContinue
}
