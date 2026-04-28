@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."

echo [start-all] Running preflight config validation...
powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%validate-runtime-config.ps1" -EnvFile "%ROOT_DIR%\.env"
if errorlevel 1 (
    echo [start-all] Preflight failed. Aborting startup.
    exit /b 1
)

echo [start-all] Starting API service...
start "javainfohunter-api" /min cmd /c "cd /d "%ROOT_DIR%" && mvn.cmd --% spring-boot:run -pl javainfohunter-api -Dspring-boot.run.profiles=develop"

echo [start-all] Starting Crawler service...
start "javainfohunter-crawler" /min cmd /c "cd /d "%ROOT_DIR%" && mvn.cmd --% spring-boot:run -pl javainfohunter-crawler -Dspring-boot.run.profiles=develop"

echo [start-all] Starting Processor service...
start "javainfohunter-processor" /min cmd /c "cd /d "%ROOT_DIR%" && mvn.cmd --% spring-boot:run -pl javainfohunter-processor -Dspring-boot.run.profiles=develop"

echo [start-all] Startup commands issued.
echo [start-all] Use stop-all.bat to stop all service windows.
exit /b 0
