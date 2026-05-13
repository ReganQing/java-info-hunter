param(
    [string]$CurrentReportFile = "",
    [string]$BaselineReportFile = "",
    [double]$CpuRegressionThresholdPercent = 25,
    [double]$MemoryRegressionThresholdPercent = 25,
    [string]$ReportFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "report-common.ps1")

if ($CurrentReportFile -eq "") {
    $CurrentReportFile = Join-Path $PSScriptRoot "runtime-baseline-report.json"
}
if ($BaselineReportFile -eq "") {
    $BaselineReportFile = Join-Path (Get-ReportArchiveDir -ScriptRoot $PSScriptRoot -ReportType "runtime-baseline") "latest.json"
}
if ($ReportFile -eq "") {
    $ReportFile = Join-Path $PSScriptRoot "runtime-baseline-compare-report.json"
}

function Get-PercentDelta {
    param(
        [double]$Current,
        [double]$Baseline
    )
    if ($Baseline -eq 0) {
        if ($Current -eq 0) {
            return 0.0
        }
        return 100.0
    }
    return [math]::Round((($Current - $Baseline) / $Baseline) * 100, 2)
}

$current = Read-JsonReport -Path $CurrentReportFile

$report = [ordered]@{
    schemaVersion = "a5-6.v1"
    reportType = "runtime-baseline-compare"
    status = "running"
    checkedAt = (Get-Date).ToString("s")
    completedAt = $null
    currentReportFile = $CurrentReportFile
    baselineReportFile = $BaselineReportFile
    thresholds = [ordered]@{
        cpuRegressionPercent = $CpuRegressionThresholdPercent
        memoryRegressionPercent = $MemoryRegressionThresholdPercent
    }
    comparison = $null
    regressions = @()
    error = $null
}

try {
    if (-not (Test-Path $BaselineReportFile)) {
        $report.status = "skipped"
        $report.comparison = [ordered]@{
            currentStatus = $current.status
            baselineStatus = "missing"
            reason = "baseline report not found"
        }
        return
    }

    $baseline = Read-JsonReport -Path $BaselineReportFile
    $currentCpu = [double]$current.safeSmoke.metrics.peakCpuPercent
    $baselineCpu = [double]$baseline.safeSmoke.metrics.peakCpuPercent
    $currentMem = [double]$current.safeSmoke.metrics.peakMemoryPercent
    $baselineMem = [double]$baseline.safeSmoke.metrics.peakMemoryPercent
    $cpuDelta = Get-PercentDelta -Current $currentCpu -Baseline $baselineCpu
    $memDelta = Get-PercentDelta -Current $currentMem -Baseline $baselineMem

    $report.comparison = [ordered]@{
        currentStatus = $current.status
        baselineStatus = $baseline.status
        currentPeakCpuPercent = $currentCpu
        baselinePeakCpuPercent = $baselineCpu
        cpuDeltaPercent = $cpuDelta
        currentPeakMemoryPercent = $currentMem
        baselinePeakMemoryPercent = $baselineMem
        memoryDeltaPercent = $memDelta
    }

    $regressions = New-Object System.Collections.Generic.List[object]
    if ($cpuDelta -gt $CpuRegressionThresholdPercent) {
        $regressions.Add([ordered]@{
            metric = "cpu"
            deltaPercent = $cpuDelta
            thresholdPercent = $CpuRegressionThresholdPercent
        }) | Out-Null
    }
    if ($memDelta -gt $MemoryRegressionThresholdPercent) {
        $regressions.Add([ordered]@{
            metric = "memory"
            deltaPercent = $memDelta
            thresholdPercent = $MemoryRegressionThresholdPercent
        }) | Out-Null
    }

    $report.regressions = @($regressions.ToArray())
    if ($regressions.Count -gt 0) {
        $report.status = "regressed"
    } else {
        $report.status = "passed"
    }
} catch {
    $report.status = "failed"
    $report.error = [ordered]@{
        message = $_.Exception.Message
        type = $_.Exception.GetType().FullName
    }
    throw
} finally {
    $report.completedAt = (Get-Date).ToString("s")
    Write-JsonReport -Path $ReportFile -Report $report
    Write-Host ("[compare-runtime-baseline] Report written to {0}" -f $ReportFile)
}
