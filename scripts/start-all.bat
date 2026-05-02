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

echo [start-all] Installing required module dependencies...
call mvnw.cmd -Dmaven.test.skip=true install -pl javainfohunter-api,javainfohunter-crawler,javainfohunter-processor -am
if errorlevel 1 (
    echo [start-all] Dependency install failed.
    exit /b 1
)

echo [start-all] Starting services with PID files...
powershell -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "$scriptDir = '%SCRIPT_DIR%';" ^
  "$rootDir = '%ROOT_DIR%';" ^
  "$services = @(" ^
  "  @{ Name = 'api'; Cmd = 'mvnw.cmd -f javainfohunter-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev -Dmaven.test.skip=true' }," ^
  "  @{ Name = 'crawler'; Cmd = 'mvnw.cmd -f javainfohunter-crawler/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev -Dmaven.test.skip=true' }," ^
  "  @{ Name = 'processor'; Cmd = 'mvnw.cmd -f javainfohunter-processor/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev -Dmaven.test.skip=true' }" ^
  ");" ^
  "foreach ($svc in $services) {" ^
  "  $logFile = Join-Path $scriptDir ('logs\\' + $svc.Name + '.log');" ^
  "  $cmd = 'cd /d \"' + $rootDir + '\" && ' + $svc.Cmd + ' > \"' + $logFile + '\" 2>&1';" ^
  "  $proc = Start-Process -FilePath 'cmd.exe' -ArgumentList '/d','/c',$cmd -PassThru -WindowStyle Hidden;" ^
  "  $pidFile = Join-Path $scriptDir ($svc.Name + '.pid');" ^
  "  Set-Content -Path $pidFile -Value $proc.Id -Encoding ascii;" ^
  "  Write-Host ('[start-all] ' + $svc.Name + ' PID=' + $proc.Id);" ^
  "}"
if errorlevel 1 (
    echo [start-all] Service start failed.
    exit /b 1
)

echo [start-all] Startup commands issued.
echo [start-all] Use stop-all.bat to stop all service windows.
exit /b 0
