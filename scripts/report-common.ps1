Set-StrictMode -Version Latest

function Write-JsonReport {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        $Report
    )

    $parent = Split-Path -Parent $Path
    if ($parent -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    $Report | ConvertTo-Json -Depth 10 | Set-Content -Path $Path -Encoding ascii
}

function Read-JsonReport {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path $Path)) {
        throw "[report] report file not found: $Path"
    }

    return (Get-Content -Path $Path -Raw | ConvertFrom-Json)
}

function Get-ReportArchiveRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptRoot
    )

    return (Join-Path $ScriptRoot "history")
}

function Get-ReportArchiveDir {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptRoot,
        [Parameter(Mandatory = $true)]
        [string]$ReportType
    )

    return (Join-Path (Get-ReportArchiveRoot -ScriptRoot $ScriptRoot) $ReportType)
}
