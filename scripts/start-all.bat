@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
set "LOG_DIR=%SCRIPT_DIR%logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo [start-all] Running preflight config validation...
set "ALLOW_PLACEHOLDER_FLAG="
if /i "%JIH_ALLOW_PLACEHOLDER_SECRETS%"=="1" set "ALLOW_PLACEHOLDER_FLAG=-AllowPlaceholderSecrets"
powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%validate-runtime-config.ps1" -EnvFile "%ROOT_DIR%\.env" %ALLOW_PLACEHOLDER_FLAG%
if errorlevel 1 (
    echo [start-all] Preflight failed. Aborting startup.
    exit /b 1
)

if /i "%JIH_SKIP_DEP_INSTALL%"=="1" (
    echo [start-all] Skipping dependency install because JIH_SKIP_DEP_INSTALL=1
) else (
    echo [start-all] Installing required module dependencies...
    call mvnw.cmd -Dmaven.test.skip=true install -pl javainfohunter-api,javainfohunter-crawler,javainfohunter-processor -am
    if errorlevel 1 (
        echo [start-all] Dependency install failed.
        exit /b 1
    )
)

echo [start-all] Starting services with PID files...
powershell -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "$scriptDir = '%SCRIPT_DIR%';" ^
  "$rootDir = '%ROOT_DIR%';" ^
  "$javaCmd = 'java';" ^
  "$services = @(" ^
  "  @{ Name = 'api'; Jar = 'javainfohunter-api\target\javainfohunter-api-0.0.1-SNAPSHOT.jar'; Args = @('--spring.profiles.active=dev') }," ^
  "  @{ Name = 'crawler'; Jar = 'javainfohunter-crawler\target\javainfohunter-crawler-0.0.1-SNAPSHOT.jar'; Args = @('--spring.profiles.active=dev') }," ^
  "  @{ Name = 'processor'; Jar = 'javainfohunter-processor\target\javainfohunter-processor-0.0.1-SNAPSHOT.jar'; Args = @('--spring.profiles.active=dev') }" ^
  ");" ^
  "foreach ($svc in $services) {" ^
  "  $jarPath = Join-Path $rootDir $svc.Jar;" ^
  "  if (-not (Test-Path $jarPath)) { throw ('[start-all] jar not found for ' + $svc.Name + ': ' + $jarPath) };" ^
  "  $logFile = Join-Path $scriptDir ('logs\\' + $svc.Name + '.log');" ^
  "  $errFile = Join-Path $scriptDir ('logs\\' + $svc.Name + '.err.log');" ^
  "  $proc = Start-Process -FilePath $javaCmd -ArgumentList (@('-jar', $jarPath) + $svc.Args) -WorkingDirectory $rootDir -PassThru -WindowStyle Hidden -RedirectStandardOutput $logFile -RedirectStandardError $errFile;" ^
  "  $pidFile = Join-Path $scriptDir ($svc.Name + '.pid');" ^
  "  Set-Content -Path $pidFile -Value $proc.Id -Encoding ascii;" ^
  "  Write-Host ('[start-all] ' + $svc.Name + ' PID=' + $proc.Id + ' JAR=' + $jarPath);" ^
  "}"
if errorlevel 1 (
    echo [start-all] Service start failed.
    exit /b 1
)

echo [start-all] Startup commands issued.
echo [start-all] Use stop-all.bat to stop all service windows.
exit /b 0
