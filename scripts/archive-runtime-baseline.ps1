param(
    [string]$SourceReportFile = "",
    [string]$ArchiveDir = "",
    [string]$SummaryReportFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "report-common.ps1")

if ($SourceReportFile -eq "") {
    $SourceReportFile = Join-Path $PSScriptRoot "runtime-baseline-report.json"
}
if ($ArchiveDir -eq "") {
    $ArchiveDir = Get-ReportArchiveDir -ScriptRoot $PSScriptRoot -ReportType "runtime-baseline"
}
if ($SummaryReportFile -eq "") {
    $SummaryReportFile = Join-Path $PSScriptRoot "runtime-baseline-archive-report.json"
}

$source = Read-JsonReport -Path $SourceReportFile
$timestamp = if ($source.completedAt) { $source.completedAt } else { $source.checkedAt }
$safeTimestamp = $timestamp.Replace(":", "-")
$archivePath = Join-Path $ArchiveDir ($safeTimestamp + ".json")
$latestPath = Join-Path $ArchiveDir "latest.json"

$summary = [ordered]@{
    schemaVersion = "a5-6.v1"
    reportType = "runtime-baseline-archive"
    status = "running"
    checkedAt = (Get-Date).ToString("s")
    completedAt = $null
    sourceReportFile = $SourceReportFile
    archiveDir = $ArchiveDir
    archivedReportFile = $archivePath
    latestReportFile = $latestPath
    archivedStatus = $source.status
    error = $null
}

try {
    $parent = Split-Path -Parent $archivePath
    if (-not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    Copy-Item -Path $SourceReportFile -Destination $archivePath -Force
    Copy-Item -Path $SourceReportFile -Destination $latestPath -Force

    $summary.status = "passed"
} catch {
    $summary.status = "failed"
    $summary.error = [ordered]@{
        message = $_.Exception.Message
        type = $_.Exception.GetType().FullName
    }
    throw
} finally {
    $summary.completedAt = (Get-Date).ToString("s")
    Write-JsonReport -Path $SummaryReportFile -Report $summary
    Write-Host ("[archive-runtime-baseline] Report written to {0}" -f $SummaryReportFile)
}
