# PostgreSQL + pgvector Docker 安装脚本
# 用于 JavaInfoHunter 项目

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host " JavaInfoHunter 数据库安装 (Docker)"  -ForegroundColor Cyan
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host ""

# 配置
$DB_NAME = "javainfohunter"
$DB_USER = "postgres"
$DB_PASSWORD = "admin123"
$PG_PORT = 5432
$CONTAINER_NAME = "pg-javainfohunter"

# 检查 Docker 是否运行
Write-Host "[1/6] 检查 Docker 状态..." -ForegroundColor Yellow
try {
    docker version | Out-Null
    Write-Host "✓ Docker 运行正常" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker 未运行，请先启动 Docker Desktop" -ForegroundColor Red
    Write-Host ""
    Write-Host "请按以下步骤操作：" -ForegroundColor Yellow
    Write-Host "1. 启动 Docker Desktop"
    Write-Host "2. 等待 Docker 完全启动（托盘图标显示绿色）"
    Write-Host "3. 重新运行此脚本"
    Write-Host ""
    Read-Host "按 Enter 键退出"
    exit 1
}

# 停止并删除旧容器
Write-Host "[2/6] 清理旧容器..." -ForegroundColor Yellow
docker ps -a --filter "name=$CONTAINER_NAME" --format "{{.Names}}" | ForEach-Object {
    docker stop $_ 2>$null
    docker rm $_ 2>$null
}

# 创建数据卷
Write-Host "[3/6] 创建数据卷..." -ForegroundColor Yellow
docker volume create pgdata 2>$null | Out-Null
Write-Host "✓ 数据卷已创建" -ForegroundColor Green

# 拉取镜像
Write-Host "[4/6] 拉取 PostgreSQL + pgvector 镜像..." -ForegroundColor Yellow
Write-Host "  (镜像大小约 500MB，首次运行需要下载)" -ForegroundColor Gray
docker pull pgvector/pgvector:pg16

# 运行容器
Write-Host "[5/6] 启动 PostgreSQL 容器..." -ForegroundColor Yellow
docker run -d `
    --name $CONTAINER_NAME `
    -e POSTGRES_USER=$DB_USER `
    -e POSTGRES_PASSWORD=$DB_PASSWORD `
    -e POSTGRES_DB=$DB_NAME `
    -p ${PG_PORT}:5432 `
    -v pgdata:/var/lib/postgresql/data `
    --restart unless-stopped `
    pgvector/pgvector:pg16

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 容器启动失败" -ForegroundColor Red
    Read-Host "按 Enter 键退出"
    exit 1
}

Write-Host "✓ 容器启动成功" -ForegroundColor Green

# 等待 PostgreSQL 就绪
Write-Host "[6/6] 等待 PostgreSQL 就绪..." -ForegroundColor Yellow
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

if ($ATTEMPT -ge $MAX_ATTEMPTS) {
    Write-Host "✗ PostgreSQL 启动超时" -ForegroundColor Red
    Read-Host "按 Enter 键退出"
    exit 1
}

# 验证 pgvector
Write-Host ""
Write-Host "验证 pgvector 扩展..." -ForegroundColor Yellow
docker exec $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "CREATE EXTENSION IF NOT EXISTS vector;" 2>$null | Out-Null

$VECTOR_VERSION = docker exec $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -t -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';" 2>$null

if ($VECTOR_VERSION) {
    Write-Host "✓ pgvector 已安装 (版本: $VECTOR_VERSION.Trim())" -ForegroundColor Green
} else {
    Write-Host "⚠ 无法验证 pgvector 版本" -ForegroundColor Yellow
}

# 设置环境变量
Write-Host ""
Write-Host "设置用户环境变量..." -ForegroundColor Yellow
[Environment]::SetEnvironmentVariable("DB_USERNAME", $DB_USER, "User")
[Environment]::SetEnvironmentVariable("DB_PASSWORD", $DB_PASSWORD, "User")
[Environment]::SetEnvironmentVariable("DB_HOST", "localhost", "User")
[Environment]::SetEnvironmentVariable("DB_PORT", "$PG_PORT", "User")
Write-Host "✓ 环境变量已设置" -ForegroundColor Green

# 显示连接信息
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
Write-Host "Docker 命令:" -ForegroundColor White
Write-Host "  docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME" -ForegroundColor Cyan
Write-Host "  docker stop $CONTAINER_NAME" -ForegroundColor Cyan
Write-Host "  docker start $CONTAINER_NAME" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "1. 重启终端（使环境变量生效）" -ForegroundColor White
Write-Host "2. 运行数据库迁移: mvnw.cmd flyway:migrate" -ForegroundColor White
Write-Host "3. 运行测试:       mvnw.cmd test" -ForegroundColor White
Write-Host ""

# 测试连接
Write-Host "测试数据库连接..." -ForegroundColor Yellow
docker exec $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "SELECT 'PostgreSQL 连接成功！' AS status;" 2>$null

Write-Host ""
Read-Host "按 Enter 键退出"
