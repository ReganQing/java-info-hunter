param(
    [int]$StartupTimeoutSeconds = 120,
    [switch]$AllowPlaceholderSecrets,
    [switch]$SkipDependencyInstall,
    [switch]$RequireHealthUp,
    [switch]$ResetRabbitTopology,
    [string]$ReportFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "report-common.ps1")

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$preflightReportFile = Join-Path $scriptDir "preflight-report.json"
if ($ReportFile -eq "") {
    $ReportFile = Join-Path $scriptDir "lifecycle-report.json"
}

$report = [ordered]@{
    schemaVersion = "a5-5.v1"
    reportType = "lifecycle"
    status = "running"
    checkedAt = (Get-Date).ToString("s")
    completedAt = $null
    parameters = [ordered]@{
        startupTimeoutSeconds = $StartupTimeoutSeconds
        allowPlaceholderSecrets = $AllowPlaceholderSecrets.IsPresent
        skipDependencyInstall = $SkipDependencyInstall.IsPresent
        requireHealthUp = $RequireHealthUp.IsPresent
        resetRabbitTopology = $ResetRabbitTopology.IsPresent
    }
    preflight = [ordered]@{
        status = "pending"
        reportFile = $preflightReportFile
    }
    rabbitTopologyReset = [ordered]@{
        status = $(if ($ResetRabbitTopology) { "pending" } else { "skipped" })
    }
    services = @(
        [ordered]@{ name = "api"; port = 8080; healthUrl = "http://localhost:8080/actuator/health"; startup = "pending"; health = $(if ($RequireHealthUp) { "pending" } else { "skipped" }) },
        [ordered]@{ name = "crawler"; port = 8081; healthUrl = "http://localhost:8081/actuator/health"; startup = "pending"; health = $(if ($RequireHealthUp) { "pending" } else { "skipped" }) },
        [ordered]@{ name = "processor"; port = 8082; healthUrl = "http://localhost:8082/actuator/health"; startup = "pending"; health = $(if ($RequireHealthUp) { "pending" } else { "skipped" }) }
    )
    shutdown = [ordered]@{
        status = "pending"
    }
    error = $null
}

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
$started = $false
try {
    if ($AllowPlaceholderSecrets) {
        & powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "validate-runtime-config.ps1") -EnvFile (Join-Path $rootDir ".env") -AllowPlaceholderSecrets -ReportFile $preflightReportFile
    } else {
        & powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "validate-runtime-config.ps1") -EnvFile (Join-Path $rootDir ".env") -ReportFile $preflightReportFile
    }
    if ($LASTEXITCODE -ne 0) {
        throw "[lifecycle] preflight config check failed."
    }
    $report.preflight.status = "passed"

    if ($ResetRabbitTopology) {
        Write-Host "[lifecycle] Resetting RabbitMQ dev topology..."
        & powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "reset-rabbitmq-topology.ps1") -EnvFile (Join-Path $rootDir ".env")
        if ($LASTEXITCODE -ne 0) {
            throw "[lifecycle] rabbit topology reset failed."
        }
        $report.rabbitTopologyReset.status = "passed"
    }

    Write-Host "[lifecycle] Starting all services..."
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
        $svcReport = @($report.services | Where-Object { $_.name -eq $svc.Name })[0]
        $svcReport.startup = "passed"
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
            $svcReport = @($report.services | Where-Object { $_.name -eq $svc.Name })[0]
            $svcReport.health = "passed"
        }
    }

    $report.shutdown.status = "pending"
    $report.status = "passed"
    Write-Host "[lifecycle] Completed successfully."
} catch {
    $report.status = "failed"
    if ($report.preflight.status -eq "pending") {
        $report.preflight.status = "failed"
    }
    if ($ResetRabbitTopology -and $report.rabbitTopologyReset.status -eq "pending") {
        $report.rabbitTopologyReset.status = "failed"
    }
    $report.error = [ordered]@{
        message = $_.Exception.Message
        type = $_.Exception.GetType().FullName
    }
    throw
}
finally {
    if ($started) {
        Write-Host "[lifecycle] Stopping all services..."
        & (Join-Path $scriptDir "stop-all.bat")
        if ($LASTEXITCODE -eq 0) {
            $report.shutdown.status = "passed"
        } else {
            $report.shutdown.status = "failed"
        }
    } else {
        $report.shutdown.status = "skipped"
    }

    if (Test-Path Env:\JIH_ALLOW_PLACEHOLDER_SECRETS) {
        Remove-Item Env:\JIH_ALLOW_PLACEHOLDER_SECRETS -ErrorAction SilentlyContinue
    }
    if (Test-Path Env:\JIH_SKIP_DEP_INSTALL) {
        Remove-Item Env:\JIH_SKIP_DEP_INSTALL -ErrorAction SilentlyContinue
    }

    $report.completedAt = (Get-Date).ToString("s")
    Write-JsonReport -Path $ReportFile -Report $report
    Write-Host ("[lifecycle] Report written to {0}" -f $ReportFile)
}
