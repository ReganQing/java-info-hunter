# PostgreSQL + pgvector Docker 安装脚本（国内镜像加速）
# 用于 JavaInfoHunter 项目

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host " JavaInfoHunter 数据库安装 (Docker)"  -ForegroundColor Cyan
Write-Host " 使用国内镜像加速"  -ForegroundColor Cyan
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host ""

# 配置
$DB_NAME = "javainfohunter"
$DB_USER = "postgres"
$DB_PASSWORD = "admin123"
$PG_PORT = 5432
$CONTAINER_NAME = "pg-javainfohunter"

# 配置国内镜像源
Write-Host "[配置] 使用阿里云镜像加速..." -ForegroundColor Yellow
$REGISTRY = "docker.mirrors.ustc.edu.cn"

# 检查 Docker
Write-Host "[1/7] 检查 Docker 状态..." -ForegroundColor Yellow
try {
    docker version | Out-Null
    Write-Host "✓ Docker 运行正常" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker 未运行" -ForegroundColor Red
    Read-Host "按 Enter 键退出"
    exit 1
}

# 清理旧容器
Write-Host "[2/7] 清理旧容器..." -ForegroundColor Yellow
docker ps -a --filter "name=$CONTAINER_NAME" --format "{{.Names}}" | ForEach-Object {
    docker stop $_ 2>$null
    docker rm $_ 2>$null
}
Write-Host "✓ 清理完成" -ForegroundColor Green

# 创建数据卷
Write-Host "[3/7] 创建数据卷..." -ForegroundColor Yellow
docker volume create pgdata 2>$null | Out-Null
Write-Host "✓ 数据卷已创建" -ForegroundColor Green

# 拉取镜像（使用加速）
Write-Host "[4/7] 拉取 PostgreSQL 镜像..." -ForegroundColor Yellow
Write-Host "  使用镜像源: $REGISTRY" -ForegroundColor Gray

# 方法1: 使用官方 PostgreSQL 镜像（无需 pgvector，纯SQL）
Write-Host "  尝试拉取官方 PostgreSQL 16 镜像..." -ForegroundColor Gray
docker pull postgres:16-alpine

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 镜像拉取失败" -ForegroundColor Red
    Write-Host ""
    Write-Host "请尝试以下方法：" -ForegroundColor Yellow
    Write-Host "1. 配置 Docker 镜像加速器：" -ForegroundColor White
    Write-Host "   https://cr.console.aliyun.com/cn-hangzhou/instances/mirrors" -ForegroundColor Cyan
    Write-Host "2. 或使用代理访问 Docker Hub" -ForegroundColor White
    Write-Host "3. 或使用 choco 本地安装 PostgreSQL" -ForegroundColor White
    Write-Host ""
    Read-Host "按 Enter 键退出"
    exit 1
}

Write-Host "✓ 镜像拉取成功" -ForegroundColor Green

# 运行容器
Write-Host "[5/7] 启动 PostgreSQL 容器..." -ForegroundColor Yellow
docker run -d `
    --name $CONTAINER_NAME `
    -e POSTGRES_USER=$DB_USER `
    -e POSTGRES_PASSWORD=$DB_PASSWORD `
    -e POSTGRES_DB=$DB_NAME `
    -p ${PG_PORT}:5432 `
    -v pgdata:/var/lib/postgresql/data `
    --restart unless-stopped `
    postgres:16-alpine

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 容器启动失败" -ForegroundColor Red
    Read-Host "按 Enter 键退出"
    exit 1
}

Write-Host "✓ 容器启动成功" -ForegroundColor Green

# 等待就绪
Write-Host "[6/7] 等待 PostgreSQL 就绪..." -ForegroundColor Yellow
$MAX_ATTEMPTS = 30
$ATTEMPT = 0

while ($ATTEMPT -lt $MAX_ATTEMPTS) {
    docker exec $CONTAINER_NAME pg_isready -U $DB_USER 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ PostgreSQL 已就绪" -ForegroundColor Green
        break
    }
    $ATTEMPT++
    Write-Host "  等待中... ($ATTEMPT/$MAX_ATTEMPTS)" -ForegroundColor Gray
    Start-Sleep -Seconds 2
}

# 设置环境变量
Write-Host "[7/7] 设置环境变量..." -ForegroundColor Yellow
[Environment]::SetEnvironmentVariable("DB_USERNAME", $DB_USER, "User")
[Environment]::SetEnvironmentVariable("DB_PASSWORD", $DB_PASSWORD, "User")
[Environment]::SetEnvironmentVariable("DB_HOST", "localhost", "User")
[Environment]::SetEnvironmentVariable("DB_PORT", "$PG_PORT", "User")
Write-Host "✓ 环境变量已设置" -ForegroundColor Green

# 显示信息
Write-Host ""
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host " 安装完成！"  -ForegroundColor Green
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host ""
Write-Host "数据库信息:" -ForegroundColor White
Write-Host "  主机:     localhost" -ForegroundColor White
Write-Host "  端口:     $PG_PORT" -ForegroundColor White
Write-Host "  数据库:   $DB_NAME" -ForegroundColor White
Write-Host "  用户名:   $DB_USER" -ForegroundColor White
Write-Host "  密码:     $DB_PASSWORD" -ForegroundColor White
Write-Host ""
Write-Host "连接字符串:" -ForegroundColor White
Write-Host "  jdbc:postgresql://localhost:$PG_PORT/$DB_NAME" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠ 注意: 当前使用的是标准 PostgreSQL 镜像" -ForegroundColor Yellow
Write-Host "  如需 pgvector 支持，请执行以下步骤：" -ForegroundColor Yellow
Write-Host "  1. 配置网络后重新运行: docker pull pgvector/pgvector:pg16" -ForegroundColor White
Write-Host "  2. 或使用项目中的 Flyway 迁移脚本（包含 pgvector 扩展安装）" -ForegroundColor White
Write-Host ""

# 测试连接
Write-Host "测试数据库连接..." -ForegroundColor Yellow
docker exec $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "SELECT version();" 2>$null

Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "1. 重启终端（使环境变量生效）" -ForegroundColor White
Write-Host "2. 运行数据库迁移: mvnw.cmd flyway:migrate" -ForegroundColor White
Write-Host "3. 运行测试:       mvnw.cmd test" -ForegroundColor White
Write-Host ""

Read-Host "按 Enter 键退出"
