@echo off
REM 切换到 UTF-8 代码页以正确显示中文
chcp 65001 >nul
REM 快速安装脚本 - 使用 Docker 安装 PostgreSQL + pgvector
REM 最简单的方式，无需编译

echo ========================================
echo JavaInfoHunter 数据库快速安装
echo (使用 Docker)
echo ========================================
echo.

REM 检查 Docker 是否安装
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ Docker 未安装
    echo.
    echo 请选择安装方式:
    echo   1. 安装 Docker (推荐): https://docs.docker.com/desktop/install/windows-install/
    echo   2. 使用 choco 安装 PostgreSQL (需要手动编译 pgvector)
    echo.
    pause
    exit /b 1
)

echo ✓ Docker 已安装
echo.

REM 配置
set DB_NAME=javainfohunter
set DB_USER=postgres
set DB_PASSWORD=admin123
set PG_PORT=5432

echo [信息] 数据库配置:
echo   数据库名: %DB_NAME%
echo   用户名: %DB_USER%
echo   密码: %DB_PASSWORD%
echo   端口: %PG_PORT%
echo.

REM 停止并删除旧容器（如果存在）
echo [1/5] 清理旧容器...
docker stop pg-javainfohunter 2>nul
docker rm pg-javainfohunter 2>nul

REM 创建 Docker volume
echo [2/5] 创建数据卷...
docker volume create pgdata >nul 2>&1

REM 运行 PostgreSQL 容器
echo [3/5] 启动 PostgreSQL + pgvector 容器...
docker run -d ^
    --name pg-javainfohunter ^
    -e POSTGRES_USER=%DB_USER% ^
    -e POSTGRES_PASSWORD=%DB_PASSWORD% ^
    -e POSTGRES_DB=%DB_NAME% ^
    -p %PG_PORT%:5432 ^
    -v pgdata:/var/lib/postgresql/data ^
    pgvector/pgvector:pg16

if %errorlevel% neq 0 (
    echo ✗ 容器启动失败
    pause
    exit /b 1
)

echo ✓ PostgreSQL 容器启动成功
echo.

REM 等待 PostgreSQL 就绪
echo [4/5] 等待 PostgreSQL 就绪...
timeout /t 5 /nobreak >nul

:waitloop
docker exec pg-javainfohunter pg_isready -U %DB_USER% >nul 2>&1
if %errorlevel% neq 0 (
    echo   等待数据库启动...
    timeout /t 2 /nobreak >nul
    goto waitloop
)

echo ✓ PostgreSQL 已就绪
echo.

REM 验证 pgvector 扩展
echo [5/5] 验证 pgvector 扩展...
docker exec pg-javainfohunter psql -U %DB_USER% -d %DB_NAME% -c "CREATE EXTENSION IF NOT EXISTS vector;" >nul 2>&1

docker exec pg-javainfohunter psql -U %DB_USER% -d %DB_NAME% -c "SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';"

if %errorlevel% equ 0 (
    echo ✓ pgvector 扩展已启用
) else (
    echo ⚠ 无法验证 pgvector
)

echo.
echo ========================================
echo 数据库安装完成！
echo ========================================
echo.

REM 设置环境变量
echo 设置环境变量...
setx DB_USERNAME "%DB_USER%" >nul
setx DB_PASSWORD "%DB_PASSWORD%" >nul
setx DB_HOST "localhost" >nul
setx DB_PORT "%PG_PORT%" >nul

echo ✓ 环境变量已设置
echo.

echo 数据库信息:
echo   主机: localhost
echo   端口: %PG_PORT%
echo   数据库: %DB_NAME%
echo   用户名: %DB_USER%
echo   密码: %DB_PASSWORD%
echo.
echo 连接字符串:
echo   jdbc:postgresql://localhost:%PG_PORT%/%DB_NAME%
echo.
echo Docker 命令:
echo   docker exec -it pg-javainfohunter psql -U %DB_USER% -d %DB_NAME%
echo   docker start pg-javainfohunter
echo   docker stop pg-javainfohunter
echo.

echo 下一步:
echo   1. 重启终端（使环境变量生效）
echo   2. 运行: mvnw.cmd flyway:migrate
echo   3. 运行: mvnw.cmd test
echo.

pause
