param(
    [int]$StartupTimeoutSeconds = 120,
    [switch]$AllowPlaceholderSecrets
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$reportFile = Join-Path $scriptDir "preflight-report.txt"

Write-Host "[lifecycle] Preflight config check..."
if ($AllowPlaceholderSecrets) {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "validate-runtime-config.ps1") -EnvFile (Join-Path $rootDir ".env") -AllowPlaceholderSecrets -ReportFile $reportFile
} else {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "validate-runtime-config.ps1") -EnvFile (Join-Path $rootDir ".env") -ReportFile $reportFile
}

Write-Host "[lifecycle] Starting all services..."
$started = $false
try {
    if ($AllowPlaceholderSecrets) {
        $env:JIH_ALLOW_PLACEHOLDER_SECRETS = "1"
    }

    & cmd /c (Join-Path $scriptDir "start-all.bat")
    if ($LASTEXITCODE -ne 0) {
        throw "[lifecycle] start-all failed."
    }
    $started = $true

    $healthUrl = "http://localhost:8080/actuator/health"
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    $healthy = $false

    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 5
            if ($resp.status -eq "UP") {
                $healthy = $true
                break
            }
        } catch {
            Start-Sleep -Seconds 2
            continue
        }
        Start-Sleep -Seconds 2
    }

    if (-not $healthy) {
        throw "[lifecycle] Health check failed: $healthUrl"
    }

    Write-Host "[lifecycle] Health check passed: $healthUrl"
    Write-Host "[lifecycle] Completed successfully."
}
finally {
    if ($started) {
        Write-Host "[lifecycle] Stopping all services..."
        & cmd /c (Join-Path $scriptDir "stop-all.bat")
    }

    if (Test-Path Env:\JIH_ALLOW_PLACEHOLDER_SECRETS) {
        Remove-Item Env:\JIH_ALLOW_PLACEHOLDER_SECRETS -ErrorAction SilentlyContinue
    }
}
