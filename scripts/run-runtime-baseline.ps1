param(
    [switch]$AllowPlaceholderSecrets,
    [switch]$SkipDependencyInstall,
    [switch]$ResetRabbitTopology,
    [switch]$RequireHealthUp,
    [string]$SmokeModule = "javainfohunter-crawler",
    [string]$SmokeTestPattern = "CrawlerHealthIndicatorTest",
    [switch]$DisableSmokeAlsoMake,
    [string]$ReportFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "report-common.ps1")

if ($ReportFile -eq "") {
    $ReportFile = Join-Path $PSScriptRoot "runtime-baseline-report.json"
}

$lifecycleReportFile = Join-Path $PSScriptRoot "lifecycle-report.json"
$safeSmokeReportFile = Join-Path $PSScriptRoot "safe-smoke-report.json"

$report = [ordered]@{
    schemaVersion = "a5-5.v1"
    reportType = "runtime-baseline"
    status = "running"
    checkedAt = (Get-Date).ToString("s")
    completedAt = $null
    lifecycleReportFile = $lifecycleReportFile
    safeSmokeReportFile = $safeSmokeReportFile
    lifecycle = $null
    safeSmoke = $null
    error = $null
}

try {
    $lifecycleArgs = @(
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "run-lifecycle-check.ps1"),
        "-ReportFile", $lifecycleReportFile
    )
    if ($AllowPlaceholderSecrets) { $lifecycleArgs += "-AllowPlaceholderSecrets" }
    if ($SkipDependencyInstall) { $lifecycleArgs += "-SkipDependencyInstall" }
    if ($ResetRabbitTopology) { $lifecycleArgs += "-ResetRabbitTopology" }
    if ($RequireHealthUp) { $lifecycleArgs += "-RequireHealthUp" }
    & powershell @lifecycleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "[runtime-baseline] lifecycle verification failed."
    }

    $smokeArgs = @(
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "safe-concurrency-smoke.ps1"),
        "-Module", $SmokeModule,
        "-TestPattern", $SmokeTestPattern,
        "-ReportFile", $safeSmokeReportFile
    )
    if ($AllowPlaceholderSecrets) { $smokeArgs += "-AllowPlaceholderSecrets" }
    if ($DisableSmokeAlsoMake) { $smokeArgs += "-DisableAlsoMake" }
    & powershell @smokeArgs
    if ($LASTEXITCODE -ne 0) {
        throw "[runtime-baseline] safe smoke verification failed."
    }

    $report.lifecycle = Read-JsonReport -Path $lifecycleReportFile
    $report.safeSmoke = Read-JsonReport -Path $safeSmokeReportFile

    if ($report.lifecycle.status -eq "passed" -and $report.safeSmoke.status -eq "passed") {
        $report.status = "passed"
    } else {
        $report.status = "failed"
    }
} catch {
    $report.status = "failed"
    $report.error = [ordered]@{
        message = $_.Exception.Message
        type = $_.Exception.GetType().FullName
    }

    if (Test-Path $lifecycleReportFile) {
        $report.lifecycle = Read-JsonReport -Path $lifecycleReportFile
    }
    if (Test-Path $safeSmokeReportFile) {
        $report.safeSmoke = Read-JsonReport -Path $safeSmokeReportFile
    }
    throw
} finally {
    $report.completedAt = (Get-Date).ToString("s")
    Write-JsonReport -Path $ReportFile -Report $report
    Write-Host ("[runtime-baseline] Report written to {0}" -f $ReportFile)
}
