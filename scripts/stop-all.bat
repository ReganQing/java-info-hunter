@echo off
REM ============================================================
REM JavaInfoHunter 一键停止脚本 (Windows)
REM ============================================================

echo.
echo ========================================
echo   停止所有 JavaInfoHunter 服务...
echo ========================================
echo.

REM 停止所有 Java 进程 (谨慎使用)
echo [INFO] 正在查找 JavaInfoHunter 进程...

REM 使用 wmic 查找并终止 Spring Boot 进程
for /f "tokens=2" %%i in ('tasklist ^| findstr /i "java.exe"') do (
    echo [终止] 进程 PID: %%i
    taskkill /F /PID %%i >nul 2>&1
)

echo.
echo ========================================
echo   所有服务已停止
echo ========================================
echo.
pause
