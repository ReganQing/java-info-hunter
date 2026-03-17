@echo off
REM 切换到 UTF-8 代码页以正确显示中文
chcp 65001 >nul
REM ============================================================
REM JavaInfoHunter 一键停止脚本 (Windows)
REM ============================================================
REM
REM 使用方法:
REM   stop-all.bat            - 交互式模式（默认）
REM   stop-all.bat auto        - 非交互式模式
REM
REM ============================================================

set MODE=%1
set INTERACTIVE=true
if /i "%MODE%"=="auto" set INTERACTIVE=false
if /i "%MODE%"=="--auto" set INTERACTIVE=false

echo.
echo ========================================
echo   停止 JavaInfoHunter 服务
echo ========================================
echo.

set SCRIPT_DIR=%~dp0

REM ============================================================
REM 方法1: 通过 PID 文件停止（如果存在）
REM ============================================================
if exist "%SCRIPT_DIR%pid\api.pid" (
    echo [1/2] 通过 PID 文件停止服务...
    for %%f in ("%SCRIPT_DIR%pid\*.pid") do (
        if exist "%%f" (
            for /f "usebackq tokens=*" %%a in (%%f) do (
                set PID=%%a
                echo   停止: %%~nxf (PID: !PID!)
                taskkill /F /PID !PID! >nul 2>&1
            )
            del "%%f"
        )
    )
    timeout /t 2 /nobreak >nul
)

REM ============================================================
REM 方法2: 通过 jps 查找并停止 JavaInfoHunter 进程
REM ============================================================
echo [2/2] 查找并停止 JavaInfoHunter 进程...
set COUNT=0
for /f "tokens=2" %%i in ('jps -l ^| findstr /i "javainfohunter"') do (
    echo   停止进程: %%i
    taskkill /F /PID %%i >nul 2>&1
    set /a COUNT+=1
)

if %COUNT%==0 (
    echo [INFO] 没有找到运行中的 JavaInfoHunter 进程
)

REM ============================================================
REM 清理日志文件（可选）
REM ============================================================
if /i "%MODE%"=="clean" (
    echo.
    echo [清理] 删除日志文件...
    if exist "%SCRIPT_DIR%api.log" del "%SCRIPT_DIR%api.log"
    if exist "%SCRIPT_DIR%crawler.log" del "%SCRIPT_DIR%crawler.log"
    if exist "%SCRIPT_DIR%processor.log" del "%SCRIPT_DIR%processor.log"
    echo [OK] 日志已清理
)

REM ============================================================
REM 完成
REM ============================================================
echo.
echo ========================================
echo   停止完成
echo ========================================
echo.
echo 已停止的服务:
if %COUNT%==0 (
    echo   (无运行中的服务)
) else (
    echo   - 共 %COUNT% 个 JavaInfoHunter 进程
)
echo.

if "%INTERACTIVE%"=="true" (
    pause
) else (
    echo 非交互式模式：停止完成
)
