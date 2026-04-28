@echo off
setlocal

set "SCRIPT_DIR=%~dp0"

echo [stop-all] Stopping JavaInfoHunter services by PID files...
powershell -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "$scriptDir = '%SCRIPT_DIR%';" ^
  "$services = @(" ^
  "  @{ Name = 'api'; Pattern = 'javainfohunter-api' }," ^
  "  @{ Name = 'crawler'; Pattern = 'javainfohunter-crawler' }," ^
  "  @{ Name = 'processor'; Pattern = 'javainfohunter-processor' }" ^
  ");" ^
  "foreach ($svc in $services) {" ^
  "  $pidFile = Join-Path $scriptDir ($svc.Name + '.pid');" ^
  "  if (-not (Test-Path $pidFile)) { Write-Host ('[stop-all] ' + $svc.Name + ' pid file not found.'); continue };" ^
  "  $pidText = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim();" ^
  "  if (-not $pidText) { Remove-Item $pidFile -ErrorAction SilentlyContinue; continue };" ^
  "  $targetPid = [int]$pidText;" ^
  "  $proc = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $targetPid) -ErrorAction SilentlyContinue;" ^
  "  if (-not $proc) { Remove-Item $pidFile -ErrorAction SilentlyContinue; Write-Host ('[stop-all] ' + $svc.Name + ' pid not running: ' + $targetPid); continue };" ^
  "  $cmd = $proc.CommandLine;" ^
  "  if (-not $cmd -or $cmd -notlike ('*' + $svc.Pattern + '*')) { Write-Warning ('[stop-all] PID ' + $targetPid + ' does not match ' + $svc.Name + ', skipping.'); continue };" ^
  "  Stop-Process -Id $targetPid -Force;" ^
  "  Remove-Item $pidFile -ErrorAction SilentlyContinue;" ^
  "  Write-Host ('[stop-all] Stopped ' + $svc.Name + ' PID=' + $targetPid);" ^
  "}"
if errorlevel 1 (
    echo [stop-all] Stop operation failed.
    exit /b 1
)

exit /b 0
