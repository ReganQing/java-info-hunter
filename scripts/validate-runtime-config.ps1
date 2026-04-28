param(
    [string]$EnvFile = ".env",
    [switch]$AllowPlaceholderSecrets,
    [string]$ReportFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Read-EnvFile {
    param([string]$Path)
    $map = @{}
    Get-Content -Path $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) {
            return
        }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        $map[$key] = $value
    }
    return $map
}

function Get-Value {
    param(
        [hashtable]$Values,
        [string]$Key,
        [string]$DefaultValue = ""
    )
    if ($Values.ContainsKey($Key) -and $Values[$Key] -ne "") {
        return $Values[$Key]
    }
    return $DefaultValue
}

function Assert-NumericRange {
    param(
        [hashtable]$Values,
        [string]$Key,
        [double]$Min,
        [double]$Max
    )
    $raw = Get-Value -Values $Values -Key $Key
    if ($raw -eq "") {
        return
    }
    $num = 0.0
    if (-not [double]::TryParse($raw, [ref]$num)) {
        throw "[config-check] $Key is not numeric: '$raw'"
    }
    if ($num -lt $Min -or $num -gt $Max) {
        throw "[config-check] $Key out of range: $num (expected $Min..$Max)"
    }
}

function Assert-Compare {
    param(
        [hashtable]$Values,
        [string]$LeftKey,
        [string]$Operator,
        [string]$RightKey
    )
    $leftRaw = Get-Value -Values $Values -Key $LeftKey
    $rightRaw = Get-Value -Values $Values -Key $RightKey
    if ($leftRaw -eq "" -or $rightRaw -eq "") {
        return
    }

    $left = 0.0
    $right = 0.0
    if (-not [double]::TryParse($leftRaw, [ref]$left)) {
        throw "[config-check] $LeftKey is not numeric: '$leftRaw'"
    }
    if (-not [double]::TryParse($rightRaw, [ref]$right)) {
        throw "[config-check] $RightKey is not numeric: '$rightRaw'"
    }

    switch ($Operator) {
        "<=" {
            if ($left -gt $right) {
                throw "[config-check] compare failed: $LeftKey($left) must be <= $RightKey($right)"
            }
        }
        ">=" {
            if ($left -lt $right) {
                throw "[config-check] compare failed: $LeftKey($left) must be >= $RightKey($right)"
            }
        }
        default {
            throw "[config-check] unsupported compare operator: $Operator"
        }
    }
}

if (-not (Test-Path $EnvFile)) {
    throw "[config-check] env file not found: $EnvFile"
}

$values = Read-EnvFile -Path $EnvFile

$required = @(
    "DB_USERNAME",
    "DB_PASSWORD",
    "RABBITMQ_HOST",
    "RABBITMQ_PORT",
    "RABBITMQ_USERNAME",
    "RABBITMQ_PASSWORD",
    "JWT_SECRET"
)

foreach ($key in $required) {
    $v = Get-Value -Values $values -Key $key
    if ($v -eq "") {
        throw "[config-check] missing required key: $key"
    }
    if (-not $AllowPlaceholderSecrets -and ($v -like "your_*" -or $v -like "*_here*")) {
        throw "[config-check] key '$key' still uses placeholder value"
    }
}

# Numeric ranges aligned with startup validators.
Assert-NumericRange -Values $values -Key "DB_POOL_MAX_SIZE" -Min 1 -Max 64
Assert-NumericRange -Values $values -Key "DB_POOL_MIN_IDLE" -Min 0 -Max 32
Assert-NumericRange -Values $values -Key "CRAWLER_RABBITMQ_CONCURRENCY" -Min 1 -Max 32
Assert-NumericRange -Values $values -Key "CRAWLER_RABBITMQ_MAX_CONCURRENCY" -Min 1 -Max 32
Assert-NumericRange -Values $values -Key "CRAWLER_RABBITMQ_PREFETCH" -Min 1 -Max 200
Assert-NumericRange -Values $values -Key "PROCESSOR_RABBITMQ_CONCURRENCY" -Min 1 -Max 32
Assert-NumericRange -Values $values -Key "PROCESSOR_RABBITMQ_MAX_CONCURRENCY" -Min 1 -Max 32
Assert-NumericRange -Values $values -Key "PROCESSOR_RABBITMQ_PREFETCH" -Min 1 -Max 200
Assert-NumericRange -Values $values -Key "CRAWLER_MAX_CONCURRENT_SOURCES" -Min 1 -Max 32
Assert-NumericRange -Values $values -Key "PROCESSING_API_CONCURRENCY_LIMIT" -Min 1 -Max 32

# Cross-field invariants.
Assert-Compare -Values $values -LeftKey "DB_POOL_MIN_IDLE" -Operator "<=" -RightKey "DB_POOL_MAX_SIZE"
Assert-Compare -Values $values -LeftKey "CRAWLER_RABBITMQ_CONCURRENCY" -Operator "<=" -RightKey "CRAWLER_RABBITMQ_MAX_CONCURRENCY"
Assert-Compare -Values $values -LeftKey "PROCESSOR_RABBITMQ_CONCURRENCY" -Operator "<=" -RightKey "PROCESSOR_RABBITMQ_MAX_CONCURRENCY"

if ($ReportFile -ne "") {
    $report = @(
        "status=passed",
        ("checked_at=" + (Get-Date).ToString("s")),
        ("env_file=" + $EnvFile),
        ("allow_placeholder_secrets=" + $AllowPlaceholderSecrets.IsPresent)
    )
    Set-Content -Path $ReportFile -Value $report -Encoding ascii
}

Write-Host "[config-check] Runtime configuration passed."
