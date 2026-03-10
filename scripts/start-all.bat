@echo off
REM ============================================================
REM JavaInfoHunter 一键启动脚本 (Windows)
REM ============================================================
REM
REM 用途: 同时启动所有 JavaInfoHunter 服务
REM
REM 前置条件:
REM   1. Java 21+ 已安装
REM   2. PostgreSQL 已启动 (localhost:5432)
REM   3. RabbitMQ 已启动 (localhost:5672)
REM   4. 环境变量已配置 (DB_PASSWORD, DASHSCOPE_API_KEY)
REM
REM ============================================================

echo.
echo ========================================
echo   JavaInfoHunter 服务启动中...
echo ========================================
echo.

REM 检查 Java 版本
echo [1/5] 检查 Java 版本...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 Java，请先安装 Java 21+
    pause
    exit /b 1
)
echo [OK] Java 已安装
echo.

REM 检查环境变量
echo [2/5] 检查环境变量...
if "%DB_PASSWORD%"=="" (
    echo [WARNING] DB_PASSWORD 未设置，使用默认值 "postgres"
    set DB_PASSWORD=postgres
)
if "%DASHSCOPE_API_KEY%"=="" (
    echo [ERROR] DASHSCOPE_API_KEY 未设置！
    echo         请设置环境变量: set DASHSCOPE_API_KEY=your_key
    pause
    exit /b 1
)
echo [OK] 环境变量检查完成
echo.

REM 检查依赖服务
echo [3/5] 检查 PostgreSQL 连接...
timeout /t 2 /nobreak >nul
echo [INFO] 请确保 PostgreSQL 已在 localhost:5432 启动
echo.

echo [4/5] 检查 RabbitMQ 连接...
echo [INFO] 请确保 RabbitMQ 已在 localhost:5672 启动
echo [INFO] 管理界面: http://localhost:15672
echo.

REM 启动服务
echo [5/5] 启动服务...
echo.

REM 启动 API 服务 (端口 8080)
echo [启动] API 服务 (端口 8080)...
start "JavaInfoHunter-API" cmd /k "mvnw.cmd spring-boot:run -pl javainfohunter-api -Dspring-boot.run.jvmArguments=\"-Dserver.port=8080\" -Dspring-boot.run.arguments=\"--spring.ai.dashscope.api-key=%DASHSCOPE_API_KEY%\""
timeout /t 15 /nobreak >nul

REM 启动 Crawler 服务
echo [启动] Crawler 服务...
start "JavaInfoHunter-Crawler" cmd /k "mvnw.cmd spring-boot:run -pl javainfohunter-crawler -Dspring-boot.run.arguments=\"--spring.ai.dashscope.api-key=%DASHSCOPE_API_KEY%\""
timeout /t 10 /nobreak >nul

REM 启动 Processor 服务
echo [启动] Processor 服务...
start "JavaInfoHunter-Processor" cmd /k "mvnw.cmd spring-boot:run -pl javainfohunter-processor -Dspring-boot.run.arguments=\"--spring.ai.dashscope.api-key=%DASHSCOPE_API_KEY%\""

echo.
echo ========================================
echo   所有服务启动完成！
echo ========================================
echo.
echo 服务地址:
echo   - API 服务:       http://localhost:8080
echo   - Swagger UI:     http://localhost:8080/swagger-ui.html
echo   - Actuator:       http://localhost:8080/actuator/health
echo   - RabbitMQ 管理:  http://localhost:15672 (admin/admin)
echo.
echo 按任意键关闭此窗口...
pause >nul
