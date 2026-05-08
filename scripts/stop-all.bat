@echo off
setlocal

set "SCRIPT_DIR=%~dp0"

echo [stop-all] Stopping JavaInfoHunter services by PID files...
powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%stop-all.ps1"
if errorlevel 1 (
    echo [stop-all] Stop operation failed.
    exit /b 1
)

exit /b 0
