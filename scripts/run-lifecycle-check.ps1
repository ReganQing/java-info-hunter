param(
    [int]$StartupTimeoutSeconds = 120,
    [switch]$AllowPlaceholderSecrets,
    [switch]$SkipDependencyInstall,
    [switch]$RequireHealthUp,
    [switch]$ResetRabbitTopology
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$reportFile = Join-Path $scriptDir "preflight-report.txt"

function Show-ServiceLogTail {
    param(
        [string]$ServiceName,
        [int]$TailLines = 80
    )
    $logFile = Join-Path $scriptDir ("logs\" + $ServiceName + ".log")
    $errFile = Join-Path $scriptDir ("logs\" + $ServiceName + ".err.log")

    if (Test-Path $logFile) {
        Write-Host ("[lifecycle] ---- {0}.log (tail {1}) ----" -f $ServiceName, $TailLines)
        Get-Content -Path $logFile -Tail $TailLines | ForEach-Object { Write-Host $_ }
    } else {
        Write-Host ("[lifecycle] log file not found: {0}" -f $logFile)
    }

    if (Test-Path $errFile) {
        Write-Host ("[lifecycle] ---- {0}.err.log (tail {1}) ----" -f $ServiceName, $TailLines)
        Get-Content -Path $errFile -Tail $TailLines | ForEach-Object { Write-Host $_ }
    }
}

Write-Host "[lifecycle] Preflight config check..."
if ($AllowPlaceholderSecrets) {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "validate-runtime-config.ps1") -EnvFile (Join-Path $rootDir ".env") -AllowPlaceholderSecrets -ReportFile $reportFile
} else {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "validate-runtime-config.ps1") -EnvFile (Join-Path $rootDir ".env") -ReportFile $reportFile
}

if ($ResetRabbitTopology) {
    Write-Host "[lifecycle] Resetting RabbitMQ dev topology..."
    & powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "reset-rabbitmq-topology.ps1") -EnvFile (Join-Path $rootDir ".env")
}

Write-Host "[lifecycle] Starting all services..."
$started = $false
try {
    if ($AllowPlaceholderSecrets) {
        $env:JIH_ALLOW_PLACEHOLDER_SECRETS = "1"
    }
    if ($SkipDependencyInstall) {
        $env:JIH_SKIP_DEP_INSTALL = "1"
    }

    & (Join-Path $scriptDir "start-all.bat")
    if ($LASTEXITCODE -ne 0) {
        throw "[lifecycle] start-all failed."
    }
    $started = $true

    $services = @(
        @{ Name = "api"; Port = 8080; Url = "http://localhost:8080/actuator/health" },
        @{ Name = "crawler"; Port = 8081; Url = "http://localhost:8081/actuator/health" },
        @{ Name = "processor"; Port = 8082; Url = "http://localhost:8082/actuator/health" }
    )
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    $notListening = @{}
    foreach ($svc in $services) {
        $notListening[$svc.Name] = $true
    }

    # Phase 1: wait ports to listen (startup success baseline)
    while ((Get-Date) -lt $deadline) {
        foreach ($svc in $services) {
            if (-not $notListening[$svc.Name]) {
                continue
            }
            try {
                $conn = Get-NetTCPConnection -LocalPort $svc.Port -State Listen -ErrorAction SilentlyContinue
                if ($conn) {
                    $notListening[$svc.Name] = $false
                }
            } catch {
                continue
            }
        }
        $remaining = @($notListening.GetEnumerator() | Where-Object { $_.Value } | ForEach-Object { $_.Key })
        if ($remaining.Count -eq 0) {
            break
        }
        Start-Sleep -Seconds 2
    }

    $startupFailed = @($notListening.GetEnumerator() | Where-Object { $_.Value } | ForEach-Object { $_.Key })
    if ($startupFailed.Count -gt 0) {
        $failedText = ($startupFailed -join ", ")
        throw "[lifecycle] Startup check failed (port not listening): $failedText"
    }

    foreach ($svc in $services) {
        Write-Host ("[lifecycle] Startup check passed: {0} (port {1})" -f $svc.Name, $svc.Port)
    }

    # Phase 2: optional strict health==UP
    if ($RequireHealthUp) {
        $unhealthy = @{}
        foreach ($svc in $services) {
            $unhealthy[$svc.Name] = $true
        }

        while ((Get-Date) -lt $deadline) {
            foreach ($svc in $services) {
                if (-not $unhealthy[$svc.Name]) {
                    continue
                }
                try {
                    $resp = Invoke-RestMethod -Uri $svc.Url -Method Get -TimeoutSec 5
                    if ($resp.status -eq "UP") {
                        $unhealthy[$svc.Name] = $false
                    }
                } catch {
                    continue
                }
            }
            $remaining = @($unhealthy.GetEnumerator() | Where-Object { $_.Value } | ForEach-Object { $_.Key })
            if ($remaining.Count -eq 0) {
                break
            }
            Start-Sleep -Seconds 2
        }

        $failed = @($unhealthy.GetEnumerator() | Where-Object { $_.Value } | ForEach-Object { $_.Key })
        if ($failed.Count -gt 0) {
            $failedText = ($failed -join ", ")
            foreach ($svcName in $failed) {
                Show-ServiceLogTail -ServiceName $svcName -TailLines 120
            }
            throw "[lifecycle] Strict health check failed for services: $failedText"
        }

        foreach ($svc in $services) {
            Write-Host ("[lifecycle] Health check passed: {0} ({1})" -f $svc.Name, $svc.Url)
        }
    }

    Write-Host "[lifecycle] Completed successfully."
}
finally {
    if ($started) {
        Write-Host "[lifecycle] Stopping all services..."
        & (Join-Path $scriptDir "stop-all.bat")
    }

    if (Test-Path Env:\JIH_ALLOW_PLACEHOLDER_SECRETS) {
        Remove-Item Env:\JIH_ALLOW_PLACEHOLDER_SECRETS -ErrorAction SilentlyContinue
    }
    if (Test-Path Env:\JIH_SKIP_DEP_INSTALL) {
        Remove-Item Env:\JIH_SKIP_DEP_INSTALL -ErrorAction SilentlyContinue
    }
}
