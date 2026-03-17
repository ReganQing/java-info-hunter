@echo off
REM 切换到 UTF-8 代码页以正确显示中文
chcp 65001 >nul
REM pgvector 扩展编译安装脚本
REM 注意：需要 Visual Studio Build Tools 和 Git

echo ========================================
echo pgvector 扩展编译安装脚本
echo ========================================
echo.

REM 检查前提条件
echo [检查] 检查必要工具...

where git >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ Git 未安装，请先安装 Git
    echo   运行: choco install git -y
    pause
    exit /b 1
)

where cmake >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ CMake 未安装，请先安装 CMake
    echo   运行: choco install cmake -y
    pause
    exit /b 1
)

echo ✓ 必要工具已安装
echo.

REM 设置 PostgreSQL 路径（根据实际安装位置调整）
set PG_PATH=C:\Program Files\PostgreSQL\16
set PG_INCLUDE=%PG_PATH%\include
set PG_LIB=%PG_PATH%\lib

echo [信息] PostgreSQL 路径: %PG_PATH%
echo.

REM 克隆 pgvector 源码
echo [1/4] 克隆 pgvector 源码...
if not exist "pgvector" (
    git clone --branch v0.5.1 https://github.com/pgvector/pgvector.git
    echo ✓ pgvector 源码克隆完成
) else (
    echo ℹ pgvector 源码已存在
)

cd pgvector

REM 编译安装
echo.
echo [2/4] 使用 CMake 生成项目...
cmake -G "Visual Studio 17 2022" -A x64 ^
    -DPOSTGRES_PATH=%PG_PATH% ^
    -DCMAKE_INSTALL_PREFIX=%PG_PATH% ^
    .

if %errorlevel% neq 0 (
    echo ✗ CMake 配置失败
    pause
    exit /b 1
)

echo ✓ CMake 配置成功
echo.
echo [3/4] 编译 pgvector...
cmake --build . --config Release

if %errorlevel% neq 0 (
    echo ✗ 编译失败
    echo   请确保已安装 Visual Studio Build Tools
    pause
    exit /b 1
)

echo ✓ pgvector 编译成功
echo.
echo [4/4] 安装 pgvector...
cmake --install . --config Release

if %errorlevel% neq 0 (
    echo ✗ 安装失败
    pause
    exit /b 1
)

echo ✓ pgvector 安装成功
echo.

REM 安装到数据库
echo [5/5] 在数据库中启用 pgvector...
set /p DB_NAME="请输入数据库名称 (默认: javainfohunter): "
if "%DB_NAME%"=="" set DB_NAME=javainfohunter

set /p DB_USER="请输入数据库用户 (默认: postgres): "
if "%DB_USER%"=="" set DB_USER=postgres

psql -U %DB_USER% -d %DB_NAME% -c "CREATE EXTENSION IF NOT EXISTS vector;"

if %errorlevel% equ 0 (
    echo ✓ pgvector 扩展已在数据库 %DB_NAME% 中启用
) else (
    echo ✗ 启用失败，请手动运行:
    echo   psql -U %DB_USER% -d %DB_NAME% -c "CREATE EXTENSION IF NOT EXISTS vector;"
)

echo.
echo ========================================
echo pgvector 安装完成！
echo ========================================
echo.
echo 验证安装:
psql -U %DB_USER% -d %DB_NAME% -c "SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';"

echo.
pause
