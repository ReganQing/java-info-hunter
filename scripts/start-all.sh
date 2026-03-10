#!/bin/bash
# ============================================================
# JavaInfoHunter 一键启动脚本 (Linux/Mac)
# ============================================================
#
# 用途: 同时启动所有 JavaInfoHunter 服务
#
# 前置条件:
#   1. Java 21+ 已安装
#   2. PostgreSQL 已启动 (localhost:5432)
#   3. RabbitMQ 已启动 (localhost:5672)
#   4. 环境变量已配置 (DB_PASSWORD, DASHSCOPE_API_KEY)
#
# 使用方法:
#   chmod +x start-all.sh
#   ./start-all.sh
#
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志目录
LOG_DIR="logs"
mkdir -p "$LOG_DIR"

echo -e "${BLUE}"
echo "========================================"
echo "  JavaInfoHunter 服务启动中..."
echo "========================================"
echo -e "${NC}"

# 1. 检查 Java 版本
echo -e "[1/5] ${BLUE}检查 Java 版本...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}[ERROR] 未找到 Java，请先安装 Java 21+${NC}"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${YELLOW}[WARNING] Java 版本低于 21，当前版本: $JAVA_VERSION${NC}"
fi
echo -e "${GREEN}[OK] Java 已安装${NC}"
echo ""

# 2. 检查环境变量
echo -e "[2/5] ${BLUE}检查环境变量...${NC}"
if [ -z "$DB_PASSWORD" ]; then
    echo -e "${YELLOW}[WARNING] DB_PASSWORD 未设置，使用默认值 'postgres'${NC}"
    export DB_PASSWORD="postgres"
fi
if [ -z "$DASHSCOPE_API_KEY" ]; then
    echo -e "${RED}[ERROR] DASHSCOPE_API_KEY 未设置！${NC}"
    echo "        请设置环境变量: export DASHSCOPE_API_KEY=your_key"
    exit 1
fi
echo -e "${GREEN}[OK] 环境变量检查完成${NC}"
echo ""

# 3. 检查依赖服务
echo -e "[3/5] ${BLUE}检查依赖服务...${NC}"

# PostgreSQL
if command -v psql &> /dev/null; then
    if psql -h localhost -U "${DB_USERNAME:-postgres}" -c '\q' &> /dev/null; then
        echo -e "${GREEN}[OK] PostgreSQL 连接正常${NC}"
    else
        echo -e "${YELLOW}[WARNING] 无法连接到 PostgreSQL${NC}"
    fi
else
    echo -e "${YELLOW}[INFO] psql 未安装，跳过 PostgreSQL 检查${NC}"
fi

# RabbitMQ
if command -v rabbitmq-diagnostics &> /dev/null; then
    echo -e "${GREEN}[OK] RabbitMQ 已安装${NC}"
else
    echo -e "${YELLOW}[INFO] 请确保 RabbitMQ 已在 localhost:5672 启动${NC}"
    echo "        管理界面: http://localhost:15672"
fi
echo ""

# 4. 清理旧 PID 文件
echo -e "[4/5] ${BLUE}清理旧进程...${NC}"
rm -f *.pid 2>/dev/null || true
echo ""

# 5. 启动服务
echo -e "[5/5] ${BLUE}启动服务...${NC}"
echo ""

# 函数: 启动服务并记录 PID
start_service() {
    local SERVICE_NAME=$1
    local MODULE=$2
    local LOG_FILE="$LOG_DIR/${SERVICE_NAME}.log"

    echo -e "${GREEN}[启动]${NC} $SERVICE_NAME..."

    # Pass DASHSCOPE_API_KEY as Spring Boot argument for all services
    # Pass DB credentials as environment variables
    DASHSCOPE_API_KEY="${DASHSCOPE_API_KEY}" \
    DB_USERNAME="${DB_USERNAME:-postgres}" \
    DB_PASSWORD="${DB_PASSWORD}" \
    nohup ./mvnw spring-boot:run -pl "$MODULE" \
        -Dmaven.test.skip=true \
        -Dspring-boot.run.arguments="--spring.ai.dashscope.api-key=${DASHSCOPE_API_KEY}" \
        > "$LOG_FILE" 2>&1 &

    local PID=$!
    echo "$PID" > "${SERVICE_NAME}.pid"
    echo -e "  日志文件: ${LOG_FILE}"
    echo -e "  进程 PID: ${PID}"

    # 等待服务启动
    sleep 5
}

# 启动 API 服务 (端口 8080)
start_service "api" "javainfohunter-api"

# 等待 API 完全启动
echo -e "${YELLOW}[等待] API 服务启动中...${NC}"
sleep 10

# 启动 Crawler 服务
start_service "crawler" "javainfohunter-crawler"
sleep 5

# 启动 Processor 服务
start_service "processor" "javainfohunter-processor"

echo ""
echo -e "${GREEN}"
echo "========================================"
echo "  所有服务启动完成！"
echo "========================================"
echo -e "${NC}"
echo ""
echo -e "${BLUE}服务地址:${NC}"
echo -e "  - API 服务:       ${GREEN}http://localhost:8080${NC}"
echo -e "  - Swagger UI:     ${GREEN}http://localhost:8080/swagger-ui.html${NC}"
echo -e "  - Actuator:       ${GREEN}http://localhost:8080/actuator/health${NC}"
echo -e "  - RabbitMQ 管理:  ${GREEN}http://localhost:15672${NC} (admin/admin)"
echo ""
echo -e "${BLUE}日志文件:${NC}"
echo "  - API:       $LOG_DIR/api.log"
echo "  - Crawler:   $LOG_DIR/crawler.log"
echo "  - Processor: $LOG_DIR/processor.log"
echo ""
echo -e "${YELLOW}停止服务: ./stop-all.sh${NC}"
echo ""
