param(
    [string]$EnvFile = ".env"
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

function Get-EnvValue {
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

function Remove-RabbitResource {
    param(
        [string]$BaseUri,
        [pscredential]$Credential,
        [string]$ResourcePath
    )

    $uri = $BaseUri.TrimEnd("/") + $ResourcePath
    try {
        Invoke-RestMethod -Uri $uri -Method Delete -Credential $Credential -TimeoutSec 10 | Out-Null
        Write-Host ("[rabbit-reset] Deleted {0}" -f $ResourcePath)
    } catch {
        $statusCode = $null
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($statusCode -eq 404) {
            Write-Host ("[rabbit-reset] Skipped missing {0}" -f $ResourcePath)
            return
        }
        throw
    }
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir

if (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile = Join-Path $rootDir $EnvFile
}

if (-not (Test-Path $EnvFile)) {
    throw "[rabbit-reset] env file not found: $EnvFile"
}

$envMap = Read-EnvFile -Path $EnvFile
$hostName = Get-EnvValue -Values $envMap -Key "RABBITMQ_HOST" -DefaultValue "localhost"
$amqpPort = Get-EnvValue -Values $envMap -Key "RABBITMQ_PORT" -DefaultValue "25672"
$managementPort = Get-EnvValue -Values $envMap -Key "RABBITMQ_MGMT_PORT" -DefaultValue ([string]([int]$amqpPort + 1))
$username = Get-EnvValue -Values $envMap -Key "RABBITMQ_USERNAME" -DefaultValue "admin"
$password = Get-EnvValue -Values $envMap -Key "RABBITMQ_PASSWORD" -DefaultValue ""
$vhost = Get-EnvValue -Values $envMap -Key "RABBITMQ_VHOST" -DefaultValue "/"

if ($password -eq "") {
    throw "[rabbit-reset] missing RABBITMQ_PASSWORD"
}

$securePassword = ConvertTo-SecureString $password -AsPlainText -Force
$credential = New-Object System.Management.Automation.PSCredential($username, $securePassword)
$encodedVhost = [System.Uri]::EscapeDataString($vhost)
$baseUri = "http://{0}:{1}/api" -f $hostName, $managementPort

$queues = @(
    "processor.raw.content.queue",
    "processor.raw.content.dlq",
    "crawler.content.encoded.queue",
    "crawler.content.encoded.dlq",
    "crawler.crawl.result.queue",
    "crawler.crawl.error.queue",
    "processor.analysis.queue",
    "processor.analysis.dlq",
    "processor.summary.queue",
    "processor.summary.dlq",
    "processor.classification.queue",
    "processor.classification.dlq",
    "processor.aggregated.queue",
    "processor.aggregated.dlq"
)

$exchanges = @(
    "crawler.direct",
    "processor.direct",
    "dead.letter.direct"
)

Write-Host ("[rabbit-reset] Resetting RabbitMQ topology via {0}" -f $baseUri)
foreach ($queue in $queues) {
    $encodedQueue = [System.Uri]::EscapeDataString($queue)
    Remove-RabbitResource -BaseUri $baseUri -Credential $credential -ResourcePath ("/queues/{0}/{1}" -f $encodedVhost, $encodedQueue)
}

foreach ($exchange in $exchanges) {
    $encodedExchange = [System.Uri]::EscapeDataString($exchange)
    Remove-RabbitResource -BaseUri $baseUri -Credential $credential -ResourcePath ("/exchanges/{0}/{1}" -f $encodedVhost, $encodedExchange)
}

Write-Host "[rabbit-reset] RabbitMQ topology reset complete."
