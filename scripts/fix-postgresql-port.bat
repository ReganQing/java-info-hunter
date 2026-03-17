@echo off
REM This script must be run as Administrator
REM It changes PostgreSQL port from 5433 to 5432

echo ===================================================
echo PostgreSQL Port Configuration Script
echo ===================================================
echo.

REM Check for administrator privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: This script must be run as Administrator
    echo Please right-click and select "Run as administrator"
    pause
    exit /b 1
)

echo [1/4] Stopping PostgreSQL service...
net stop postgresql-x64-18
if %errorLevel% neq 0 (
    echo WARNING: Failed to stop service or service already stopped
)

echo.
echo [2/4] Updating postgresql.conf...
powershell -Command "(Get-Content 'C:\Program Files\PostgreSQL\18\data\postgresql.conf') -replace 'port = 5433', 'port = 5432' | Set-Content 'C:\Program Files\PostgreSQL\18\data\postgresql.conf'"

echo.
echo [3/4] Starting PostgreSQL service...
net start postgresql-x64-18

echo.
echo [4/4] Verifying configuration...
timeout /t 3 /nobreak >nul
netstat -ano | findstr ":5432"

echo.
echo ===================================================
echo PostgreSQL should now be running on port 5432
echo ===================================================
echo.
pause
