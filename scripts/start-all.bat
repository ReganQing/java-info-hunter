@echo off
REM 切换到 UTF-8 代码页以正确显示中文
chcp 65001 >nul
REM ============================================================
REM JavaInfoHunter 一键启动脚本 (Windows)
REM ============================================================
REM
REM 用途: 同时启动所有 JavaInfoHunter 服务
REM
REM 使用方法:
REM   start-all.bat           - 交互式模式（默认，需要按键关闭窗口）
REM   start-all.bat auto       - 非交互式模式（后台启动，自动关闭）
REM   start-all.bat stop       - 停止所有服务
REM
REM 前置条件:
REM   1. Java 21+ 已安装
REM   2. PostgreSQL 已启动 (localhost:5432)
REM   3. RabbitMQ 已启动 (localhost:25672)
REM   4. 环境变量已配置 (DB_PASSWORD, DASHSCOPE_API_KEY)
REM
REM ============================================================

set MODE=%1
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%.."

REM ============================================================
REM 处理停止命令
REM ============================================================
if /i "%MODE%"=="stop" goto :stop_services
if /i "%MODE%"=="--stop" goto :stop_services

REM ============================================================
REM 检查模式
REM ============================================================
set INTERACTIVE=true
if /i "%MODE%"=="auto" set INTERACTIVE=false
if /i "%MODE%"=="--auto" set INTERACTIVE=false

echo.
echo ========================================
echo   JavaInfoHunter 服务启动
echo ========================================
echo.

REM ============================================================
REM 检查 Java 版本
REM ============================================================
echo [1/6] 检查 Java 版本...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 Java，请先安装 Java 21+
    if "%INTERACTIVE%"=="true" pause
    exit /b 1
)
echo [OK] Java 已安装
echo.

REM ============================================================
REM 检查环境变量
REM ============================================================
echo [2/6] 检查环境变量...
if "%DB_PASSWORD%"=="" (
    echo [WARNING] DB_PASSWORD 未设置，使用默认值
    set DB_PASSWORD=admin6866!@#
)
if "%DASHSCOPE_API_KEY%"=="" (
    echo [ERROR] DASHSCOPE_API_KEY 未设置！
    echo         请设置环境变量: set DASHSCOPE_API_KEY=your_key
    if "%INTERACTIVE%"=="true" pause
    exit /b 1
)
echo [OK] 环境变量检查完成
echo.

REM ============================================================
REM 检查依赖服务
REM ============================================================
echo [3/6] 检查依赖服务...

REM 检查 PostgreSQL
timeout /t 1 /nobreak >nul
PGPASSWORD=%DB_PASSWORD% psql -h localhost -p 5432 -U admin -c "SELECT 1" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] PostgreSQL 连接失败，请确保已启动
) else (
    echo [OK] PostgreSQL 已连接
)

REM 检查 RabbitMQ
timeout /t 1 /nobreak >nul
curl -s http://localhost:25673 >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] RabbitMQ 管理界面无法访问，请确保已启动
    echo         端口: localhost:25672 (AMQP), localhost:25673 (管理)
) else (
    echo [OK] RabbitMQ 已运行
)

echo.

REM ============================================================
REM 杀掉已存在的服务进程
REM ============================================================
echo [4/6] 清理已存在的服务...
for /f "tokens=2" %%i in ('jps -l ^| findstr /i "javainfohunter"') do (
    echo   停止进程: %%i
    taskkill /F /PID %%i >nul 2>&1
)
timeout /t 2 /nobreak >nul
echo [OK] 清理完成
echo.

REM ============================================================
REM 创建 PID 文件目录
REM ============================================================
if not exist "%SCRIPT_DIR%pid" mkdir "%SCRIPT_DIR%pid"

REM ============================================================
REM 启动服务（后台模式）
REM ============================================================
echo [5/6] 启动服务...
echo.

REM 启动 API 服务 (端口 8080)
echo [启动] API 服务 (端口 8080)...
if "%INTERACTIVE%"=="true" (
    start "JavaInfoHunter-API" cmd /k "cd /d \"%CD%\" && mvnw.cmd spring-boot:run -pl javainfohunter-api -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=\"--spring.ai.dashscope.api-key=%DASHSCOPE_API_KEY%\""
) else (
    start /B cmd /c "cd /d \"%CD%\" && mvnw.cmd spring-boot:run -pl javainfohunter-api -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=\"--spring.ai.dashscope.api-key=%DASHSCOPE_API_KEY%\" > \"%SCRIPT_DIR%api.log\" 2>&1" && echo %%TIME%% > "%SCRIPT_DIR%pid\api.pid"
)
timeout /t 15 /nobreak >nul

REM 启动 Crawler 服务
echo [启动] Crawler 服务 (端口 8081)...
if "%INTERACTIVE%"=="true" (
    start "JavaInfoHunter-Crawler" cmd /k "cd /d \"%CD%\" && mvnw.cmd spring-boot:run -pl javainfohunter-crawler -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=\"--spring.ai.dashscope.api-key=%DASHSCOPE_API_KEY%\""
) else (
    start /B cmd /c "cd /d \"%CD%\" && mvnw.cmd spring-boot:run -pl javainfohunter-crawler -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=\"--spring.ai.dashscope.api-key=%DASHSCOPE_API_KEY%\" > \"%SCRIPT_DIR%crawler.log\" 2>&1" && echo %%TIME%% > "%SCRIPT_DIR%pid\crawler.pid"
)
timeout /t 10 /nobreak >nul

REM 启动 Processor 服务 (端口 8082)
echo [启动] Processor 服务 (端口 8082)...
if "%INTERACTIVE%"=="true" (
    start "JavaInfoHunter-Processor" cmd /k "cd /d \"%CD%\" && mvnw.cmd spring-boot:run -pl javainfohunter-processor -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=\"--spring.ai.dashscope.api-key=%DASHSCOPE_API_KEY%\""
) else (
    start /B cmd /c "cd /d \"%CD%\" && mvnw.cmd spring-boot:run -pl javainfohunter-processor -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=\"--spring.ai.dashscope.api-key=%DASHSCOPE_API_KEY%\" > \"%SCRIPT_DIR%processor.log\" 2>&1" && echo %%TIME%% > "%SCRIPT_DIR%pid\processor.pid"
)

echo.
echo [6/6] 等待服务启动...
timeout /t 30 /nobreak >nul

REM ============================================================
REM 检查服务状态
REM ============================================================
echo.
echo ========================================
echo   服务启动完成！
echo ========================================
echo.

echo 服务地址:
echo   - API 服务:       http://localhost:8080
echo   - Crawler 服务:   http://localhost:8081
echo   - Processor 服务: http://localhost:8082
echo   - Swagger UI:     http://localhost:8080/swagger-ui.html
echo   - Actuator (API):  http://localhost:8080/actuator/health
echo   - RabbitMQ 管理:  http://localhost:25673 (guest/guest)
echo.

echo 日志文件:
echo   - API:      %SCRIPT_DIR%api.log
echo   - Crawler:  %SCRIPT_DIR%crawler.log
echo   - Processor: %SCRIPT_DIR%processor.log
echo.

echo PID 文件:
echo   - API:      %SCRIPT_DIR%pid\api.pid
echo   - Crawler:  %SCRIPT_DIR%pid\crawler.pid
echo   - Processor: %SCRIPT_DIR%pid\processor.pid
echo.

echo 使用命令停止服务:
echo   start-all.bat stop
echo.

if "%INTERACTIVE%"=="true" (
    echo 按任意键关闭此窗口...
    pause >nul
    goto :eof
)

echo 非交互式模式：服务已在后台启动
echo 查看日志: type api.log ^| more
goto :eof

REM ============================================================
REM 停止服务
REM ============================================================
:stop_services
echo.
echo ========================================
echo   停止 JavaInfoHunter 服务
echo ========================================
echo.

echo [1/3] 停止 Java 进程...
for /f "tokens=2" %%i in ('jps -l ^| findstr /i "javainfohunter"') do (
    echo   停止进程: %%i
    taskkill /F /PID %%i >nul 2>&1
)

echo [2/3] 清理 PID 文件...
if exist "%SCRIPT_DIR%pid\api.pid" del "%SCRIPT_DIR%pid\api.pid"
if exist "%SCRIPT_DIR%pid\crawler.pid" del "%SCRIPT_DIR%pid\crawler.pid"
if exist "%SCRIPT_DIR%pid\processor.pid" del "%SCRIPT_DIR%pid\processor.pid"

echo [3/3] 清理日志文件...
if exist "%SCRIPT_DIR%api.log" del "%SCRIPT_DIR%api.log"
if exist "%SCRIPT_DIR%crawler.log" del "%SCRIPT_DIR%crawler.log"
if exist "%SCRIPT_DIR%processor.log" del "%SCRIPT_DIR%processor.log

echo.
echo [OK] 所有服务已停止
echo.
goto :eof
