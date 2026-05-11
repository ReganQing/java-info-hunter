Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$services = @(
    @{
        Name = "api"
        MatchTokens = @(
            "javainfohunter-api-0.0.1-SNAPSHOT.jar",
            "ApiApplication"
        )
    },
    @{
        Name = "crawler"
        MatchTokens = @(
            "javainfohunter-crawler-0.0.1-SNAPSHOT.jar",
            "CrawlerApplication"
        )
    },
    @{
        Name = "processor"
        MatchTokens = @(
            "javainfohunter-processor-0.0.1-SNAPSHOT.jar",
            "ProcessorApplication"
        )
    }
)

function Test-ProcessMatchesService {
    param(
        $ProcessInfo,
        [string[]]$MatchTokens
    )

    if (-not $ProcessInfo -or -not $ProcessInfo.CommandLine) {
        return $false
    }

    foreach ($token in $MatchTokens) {
        if ($ProcessInfo.CommandLine -like ("*" + $token + "*")) {
            return $true
        }
    }

    return $false
}

foreach ($svc in $services) {
    $pidFile = Join-Path $scriptDir ($svc.Name + ".pid")
    if (-not (Test-Path $pidFile)) {
        $fallbackNoPid = Get-CimInstance Win32_Process | Where-Object {
            Test-ProcessMatchesService -ProcessInfo $_ -MatchTokens $svc.MatchTokens
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

    if (Test-ProcessMatchesService -ProcessInfo $proc -MatchTokens $svc.MatchTokens) {
        Stop-Process -Id $targetPid -Force -ErrorAction SilentlyContinue
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        Write-Host ("[stop-all] Stopped {0} PID={1}" -f $svc.Name, $targetPid)
        continue
    }

    $fallback = Get-CimInstance Win32_Process | Where-Object {
        Test-ProcessMatchesService -ProcessInfo $_ -MatchTokens $svc.MatchTokens
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
