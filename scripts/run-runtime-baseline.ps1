param(
    [switch]$AllowPlaceholderSecrets,
    [switch]$SkipDependencyInstall,
    [switch]$ResetRabbitTopology,
    [switch]$RequireHealthUp,
    [string]$SmokeModule = "javainfohunter-crawler",
    [string]$SmokeTestPattern = "CrawlerHealthIndicatorTest",
    [switch]$DisableSmokeAlsoMake,
    [switch]$ArchiveHistory,
    [switch]$CompareAgainstLatest,
    [double]$CpuRegressionThresholdPercent = 25,
    [double]$MemoryRegressionThresholdPercent = 25,
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
$archiveReportFile = Join-Path $PSScriptRoot "runtime-baseline-archive-report.json"
$compareReportFile = Join-Path $PSScriptRoot "runtime-baseline-compare-report.json"

$report = [ordered]@{
    schemaVersion = "a5-6.v1"
    reportType = "runtime-baseline"
    status = "running"
    checkedAt = (Get-Date).ToString("s")
    completedAt = $null
    lifecycleReportFile = $lifecycleReportFile
    safeSmokeReportFile = $safeSmokeReportFile
    archiveReportFile = $archiveReportFile
    compareReportFile = $compareReportFile
    lifecycle = $null
    safeSmoke = $null
    archive = $null
    comparison = $null
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

    $report.completedAt = (Get-Date).ToString("s")
    Write-JsonReport -Path $ReportFile -Report $report

    if ($ArchiveHistory) {
        & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "archive-runtime-baseline.ps1") -SourceReportFile $ReportFile -SummaryReportFile $archiveReportFile
        if ($LASTEXITCODE -ne 0) {
            throw "[runtime-baseline] archive step failed."
        }
        $report.archive = Read-JsonReport -Path $archiveReportFile
    }

    if ($CompareAgainstLatest) {
        $compareArgs = @(
            "-ExecutionPolicy", "Bypass",
            "-File", (Join-Path $PSScriptRoot "compare-runtime-baseline.ps1"),
            "-CurrentReportFile", $ReportFile,
            "-ReportFile", $compareReportFile,
            "-CpuRegressionThresholdPercent", $CpuRegressionThresholdPercent,
            "-MemoryRegressionThresholdPercent", $MemoryRegressionThresholdPercent
        )
        & powershell @compareArgs
        if ($LASTEXITCODE -ne 0) {
            throw "[runtime-baseline] compare step failed."
        }
        $report.comparison = Read-JsonReport -Path $compareReportFile
        if ($report.comparison.status -eq "regressed") {
            $report.status = "regressed"
        }
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
    if (Test-Path $archiveReportFile) {
        $report.archive = Read-JsonReport -Path $archiveReportFile
    }
    if (Test-Path $compareReportFile) {
        $report.comparison = Read-JsonReport -Path $compareReportFile
    }
    throw
} finally {
    $report.completedAt = (Get-Date).ToString("s")
    Write-JsonReport -Path $ReportFile -Report $report
    Write-Host ("[runtime-baseline] Report written to {0}" -f $ReportFile)
}
