# RabbitMQ Docker 安装脚本
# 用于 JavaInfoHunter 项目

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host " JavaInfoHunter RabbitMQ 安装 (Docker)"  -ForegroundColor Cyan
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host ""

# 配置
$RABBITMQ_USER = "admin"
$RABBITMQ_PASSWORD = "admin123"
$AMQP_PORT = 5672
$MGMT_PORT = 15672
$CONTAINER_NAME = "rabbitmq-javainfohunter"
$IMAGE_NAME = "rabbitmq:3-management"

# 检查 Docker 是否运行
Write-Host "[1/7] 检查 Docker 状态..." -ForegroundColor Yellow
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
Write-Host "[2/7] 清理旧容器..." -ForegroundColor Yellow
docker ps -a --filter "name=$CONTAINER_NAME" --format "{{.Names}}" | ForEach-Object {
    docker stop $_ 2>$null
    docker rm $_ 2>$null
    Write-Host "  已删除旧容器: $_" -ForegroundColor Gray
}
Write-Host "✓ 清理完成" -ForegroundColor Green

# 创建数据卷
Write-Host "[3/7] 创建数据卷..." -ForegroundColor Yellow
docker volume create rabbitmq-data 2>$null | Out-Null
Write-Host "✓ 数据卷已创建: rabbitmq-data" -ForegroundColor Green

# 拉取镜像
Write-Host "[4/7] 拉取 RabbitMQ 镜像..." -ForegroundColor Yellow
Write-Host "  (镜像大小约 200MB，首次运行需要下载)" -ForegroundColor Gray
Write-Host "  镜像: $IMAGE_NAME" -ForegroundColor Cyan

$pullProgress = 0
docker pull $IMAGE_NAME

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 镜像拉取失败" -ForegroundColor Red
    Write-Host ""
    Write-Host "可能的原因：" -ForegroundColor Yellow
    Write-Host "1. 网络连接问题" -ForegroundColor White
    Write-Host "2. Docker Hub 无法访问" -ForegroundColor White
    Write-Host ""
    Write-Host "解决方案：" -ForegroundColor Yellow
    Write-Host "1. 配置 Docker 镜像加速器（参考 scripts/README.md）" -ForegroundColor White
    Write-Host "2. 检查网络连接" -ForegroundColor White
    Write-Host "3. 重试安装" -ForegroundColor White
    Write-Host ""
    Read-Host "按 Enter 键退出"
    exit 1
}

Write-Host "✓ 镜像拉取成功" -ForegroundColor Green

# 运行容器
Write-Host "[5/7] 启动 RabbitMQ 容器..." -ForegroundColor Yellow
Write-Host "  配置参数:" -ForegroundColor Gray
Write-Host "    - 容器名:     $CONTAINER_NAME" -ForegroundColor Gray
Write-Host "    - AMQP 端口:  $AMQP_PORT" -ForegroundColor Gray
Write-Host "    - 管理端口:   $MGMT_PORT" -ForegroundColor Gray
Write-Host "    - 数据卷:     rabbitmq-data" -ForegroundColor Gray
Write-Host "    - 用户名:     $RABBITMQ_USER" -ForegroundColor Gray

docker run -d `
    --name $CONTAINER_NAME `
    -p ${AMQP_PORT}:5672 `
    -p ${MGMT_PORT}:15672 `
    -e RABBITMQ_DEFAULT_USER=$RABBITMQ_USER `
    -e RABBITMQ_DEFAULT_PASS=$RABBITMQ_PASSWORD `
    -v rabbitmq-data:/var/lib/rabbitmq `
    --hostname rabbitmq-javainfohunter `
    --restart unless-stopped `
    $IMAGE_NAME

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 容器启动失败" -ForegroundColor Red
    Write-Host ""
    Write-Host "可能的原因：" -ForegroundColor Yellow
    Write-Host "1. 端口 $AMQP_PORT 或 $MGMT_PORT 已被占用" -ForegroundColor White
    Write-Host "2. 容器名称冲突" -ForegroundColor White
    Write-Host ""
    Write-Host "检查命令：" -ForegroundColor Yellow
    Write-Host "  netstat -ano | findstr $AMQP_PORT" -ForegroundColor Cyan
    Write-Host "  netstat -ano | findstr $MGMT_PORT" -ForegroundColor Cyan
    Write-Host ""
    Read-Host "按 Enter 键退出"
    exit 1
}

Write-Host "✓ 容器启动成功" -ForegroundColor Green

# 等待 RabbitMQ 就绪
Write-Host "[6/7] 等待 RabbitMQ 就绪..." -ForegroundColor Yellow
$MAX_ATTEMPTS = 30
$ATTEMPT = 0
$READY = $false

while ($ATTEMPT -lt $MAX_ATTEMPTS) {
    $ATTEMPT++

    # 检查容器状态
    $containerStatus = docker inspect --format='{{.State.Status}}' $CONTAINER_NAME 2>$null

    if ($containerStatus -ne "running") {
        Write-Host "✗ 容器未运行 (状态: $containerStatus)" -ForegroundColor Red
        Write-Host ""
        Write-Host "查看日志：" -ForegroundColor Yellow
        Write-Host "  docker logs $CONTAINER_NAME" -ForegroundColor Cyan
        Write-Host ""
        Read-Host "按 Enter 键退出"
        exit 1
    }

    # 检查 RabbitMQ 是否就绪
    $healthCheck = docker exec $CONTAINER_NAME rabbitmq-diagnostics check_running 2>$null

    if ($LASTEXITCODE -eq 0) {
        $READY = $true
        Write-Host "✓ RabbitMQ 已就绪" -ForegroundColor Green
        break
    }

    Write-Host "  等待中... ($ATTEMPT/$MAX_ATTEMPTS)" -ForegroundColor Gray
    Start-Sleep -Seconds 1
}

if (-not $READY) {
    Write-Host "✗ RabbitMQ 启动超时" -ForegroundColor Red
    Write-Host ""
    Write-Host "查看日志以获取更多信息：" -ForegroundColor Yellow
    Write-Host "  docker logs $CONTAINER_NAME" -ForegroundColor Cyan
    Write-Host ""
    Read-Host "按 Enter 键退出"
    exit 1
}

# 验证安装
Write-Host "[7/7] 验证安装..." -ForegroundColor Yellow

# 获取 RabbitMQ 版本
$versionInfo = docker exec $CONTAINER_NAME rabbitmqctl version 2>$null
if ($versionInfo) {
    Write-Host "✓ RabbitMQ 版本: $versionInfo" -ForegroundColor Green
} else {
    Write-Host "⚠ 无法获取版本信息" -ForegroundColor Yellow
}

# 检查管理插件
$plugins = docker exec $CONTAINER_NAME rabbitmq-plugins list -e 2>$null | Select-String "rabbitmq_management"
if ($plugins) {
    Write-Host "✓ 管理插件已启用" -ForegroundColor Green
} else {
    Write-Host "⚠ 管理插件未启用" -ForegroundColor Yellow
}

# 设置环境变量
Write-Host ""
Write-Host "设置用户环境变量..." -ForegroundColor Yellow
[Environment]::SetEnvironmentVariable("RABBITMQ_HOST", "localhost", "User")
[Environment]::SetEnvironmentVariable("RABBITMQ_PORT", "$AMQP_PORT", "User")
[Environment]::SetEnvironmentVariable("RABBITMQ_USER", $RABBITMQ_USER, "User")
[Environment]::SetEnvironmentVariable("RABBITMQ_PASSWORD", $RABBITMQ_PASSWORD, "User")
[Environment]::SetEnvironmentVariable("RABBITMQ_MANAGEMENT_PORT", "$MGMT_PORT", "User")
Write-Host "✓ 环境变量已设置" -ForegroundColor Green

# 显示连接信息
Write-Host ""
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host " 安装完成！"  -ForegroundColor Green
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host ""
Write-Host "RabbitMQ 信息:" -ForegroundColor White
Write-Host "  容器名:     $CONTAINER_NAME" -ForegroundColor White
Write-Host "  主机:       localhost" -ForegroundColor White
Write-Host "  AMQP 端口:  $AMQP_PORT" -ForegroundColor White
Write-Host "  管理端口:   $MGMT_PORT" -ForegroundColor White
Write-Host "  用户名:     $RABBITMQ_USER" -ForegroundColor White
Write-Host "  密码:       $RABBITMQ_PASSWORD" -ForegroundColor White
Write-Host ""
Write-Host "连接字符串:" -ForegroundColor White
Write-Host "  AMQP:      amqp://$RABBITMQ_USER:$RABBITMQ_PASSWORD@localhost:$AMQP_PORT" -ForegroundColor Cyan
Write-Host ""
Write-Host "管理界面:" -ForegroundColor White
Write-Host "  URL:       http://localhost:$MGMT_PORT" -ForegroundColor Cyan
Write-Host "  用户名:    $RABBITMQ_USER" -ForegroundColor Cyan
Write-Host "  密码:      $RABBITMQ_PASSWORD" -ForegroundColor Cyan
Write-Host ""
Write-Host "Spring Boot 配置:" -ForegroundColor White
Write-Host "  spring.rabbitmq.host=localhost" -ForegroundColor Cyan
Write-Host "  spring.rabbitmq.port=$AMQP_PORT" -ForegroundColor Cyan
Write-Host "  spring.rabbitmq.username=$RABBITMQ_USER" -ForegroundColor Cyan
Write-Host "  spring.rabbitmq.password=$RABBITMQ_PASSWORD" -ForegroundColor Cyan
Write-Host ""
Write-Host "常用 Docker 命令:" -ForegroundColor White
Write-Host "  查看日志:     docker logs -f $CONTAINER_NAME" -ForegroundColor Cyan
Write-Host "  进入容器:     docker exec -it $CONTAINER_NAME bash" -ForegroundColor Cyan
Write-Host "  停止容器:     docker stop $CONTAINER_NAME" -ForegroundColor Cyan
Write-Host "  启动容器:     docker start $CONTAINER_NAME" -ForegroundColor Cyan
Write-Host "  重启容器:     docker restart $CONTAINER_NAME" -ForegroundColor Cyan
Write-Host "  删除容器:     docker rm -f $CONTAINER_NAME" -ForegroundColor Cyan
Write-Host "  查看状态:     docker exec $CONTAINER_NAME rabbitmqctl status" -ForegroundColor Cyan
Write-Host ""
Write-Host "RabbitMQ 管理命令:" -ForegroundColor White
Write-Host "  列出队列:     docker exec $CONTAINER_NAME rabbitmqctl list_queues" -ForegroundColor Cyan
Write-Host "  列出用户:     docker exec $CONTAINER_NAME rabbitmqctl list_users" -ForegroundColor Cyan
Write-Host "  列出连接:     docker exec $CONTAINER_NAME rabbitmqctl list_connections" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "1. 重启终端（使环境变量生效）" -ForegroundColor White
Write-Host "2. 访问管理界面: http://localhost:$MGMT_PORT" -ForegroundColor White
Write-Host "3. 在 Spring Boot 中配置 RabbitMQ 连接" -ForegroundColor White
Write-Host "4. 运行测试:       mvnw.cmd test" -ForegroundColor White
Write-Host ""

# 测试连接
Write-Host "测试 AMQP 连接..." -ForegroundColor Yellow
try {
    $testResult = docker exec $CONTAINER_NAME rabbitmqctl status 2>$null | Select-String "listeners,.*{"
    if ($testResult) {
        Write-Host "✓ AMQP 服务运行正常" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠ 无法验证 AMQP 连接" -ForegroundColor Yellow
}

Write-Host ""
Read-Host "按 Enter 键退出"
