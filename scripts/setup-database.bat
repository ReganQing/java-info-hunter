@echo off
REM 切换到 UTF-8 代码页以正确显示中文
chcp 65001 >nul
REM PostgreSQL 数据库环境设置脚本
REM 用于 JavaInfoHunter 项目

echo ========================================
echo JavaInfoHunter 数据库设置脚本
echo ========================================
echo.

REM 配置变量
set DB_PASSWORD=admin123
set DB_NAME=javainfohunter
set DB_USER=postgres

echo [1/7] 检查 PostgreSQL 服务状态...
sc query postgresql-x64-16 | find "RUNNING"
if %errorlevel% equ 0 (
    echo ✓ PostgreSQL 服务正在运行
) else (
    echo ✗ PostgreSQL 服务未运行，正在启动...
    net start postgresql-x64-16
)

echo.
echo [2/7] 创建数据库 %DB_NAME%...
createdb -U %DB_USER% %DB_NAME% 2>nul
if %errorlevel% equ 0 (
    echo ✓ 数据库 %DB_NAME% 创建成功
) else (
    echo ℹ 数据库 %DB_NAME% 可能已存在
)

echo.
echo [3/7] 安装 pgvector 扩展...
psql -U %DB_USER% -d %DB_NAME% -c "CREATE EXTENSION IF NOT EXISTS vector;" 2>nul
if %errorlevel% equ 0 (
    echo ✓ pgvector 扩展安装成功
) else (
    echo ✗ pgvector 扩展安装失败
    echo   请手动运行: psql -U postgres -d javainfohunter -c "CREATE EXTENSION IF NOT EXISTS vector;"
)

echo.
echo [4/7] 验证 pgvector 安装...
psql -U %DB_USER% -d %DB_NAME% -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';"
if %errorlevel% equ 0 (
    echo ✓ pgvector 已安装
) else (
    echo ⚠ 无法验证 pgvector 版本
)

echo.
echo [5/7] 设置系统环境变量...
setx DB_USERNAME "%DB_USER%" >nul
setx DB_PASSWORD "%DB_PASSWORD%" >nul
echo ✓ 环境变量已设置:
echo   DB_USERNAME = %DB_USER%
echo   DB_PASSWORD = %DB_PASSWORD% (请勿泄露)

echo.
echo [6/7] 测试数据库连接...
psql -U %DB_USER% -d %DB_NAME% -c "SELECT version();" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ 数据库连接成功
) else (
    echo ⚠ 数据库连接失败，请检查密码是否正确
)

echo.
echo [7/7] 创建测试表...
psql -U %DB_USER% -d %DB_NAME% -c "CREATE TABLE IF NOT EXISTS test_table (id SERIAL PRIMARY KEY, name VARCHAR(100));" 2>nul
if %errorlevel% equ 0 (
    echo ✓ 测试表创建成功
    psql -U %DB_USER% -d %DB_NAME% -c "DROP TABLE test_table;"
) else (
    echo ⚠ 测试表创建失败
)

echo.
echo ========================================
echo 数据库设置完成！
echo ========================================
echo.
echo 数据库信息:
echo   主机: localhost
echo   端口: 5432
echo   数据库: %DB_NAME%
echo   用户名: %DB_USER%
echo   密码: %DB_PASSWORD%
echo.
echo 连接字符串:
echo   jdbc:postgresql://localhost:5432/%DB_NAME%
echo.
echo 常用命令:
echo   psql -U %DB_USER% -d %DB_NAME%     # 连接数据库
echo   psql -U %DB_USER% -d %DB_NAME% -f file.sql  # 执行 SQL 文件
echo.
echo 下一步:
echo   1. 重启终端（使环境变量生效）
echo   2. 运行: mvnw.cmd flyway:migrate
echo   3. 运行: mvnw.cmd test
echo.

pause
