#!/bin/bash
# ============================================================
# JavaInfoHunter 一键停止脚本 (Linux/Mac)
# ============================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "========================================"
echo "  停止所有 JavaInfoHunter 服务..."
echo "========================================"
echo -e "${NC}"

# 函数: 根据 PID 文件停止服务
stop_service_by_pid() {
    local SERVICE_NAME=$1
    local PID_FILE="${SERVICE_NAME}.pid"

    if [ -f "$PID_FILE" ]; then
        local PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo -e "${GREEN}[停止]${NC} $SERVICE_NAME (PID: $PID)"
            kill "$PID"
            rm -f "$PID_FILE"
        else
            echo -e "${YELLOW}[跳过]${NC} $SERVICE_NAME (进程不存在)"
            rm -f "$PID_FILE"
        fi
    fi
}

# 停止各服务
stop_service_by_pid "api"
stop_service_by_pid "crawler"
stop_service_by_pid "processor"

# 额外检查: 停止可能遗留的 Spring Boot 进程
echo ""
echo -e "${BLUE}[清理]${NC} 查找遗留的 Java 进程..."
pkill -f "spring-boot:run" 2>/dev/null && echo -e "${GREEN}[OK]${NC} 已清理遗留进程" || echo -e "${YELLOW}[INFO]${NC} 无遗留进程"

echo ""
echo -e "${GREEN}"
echo "========================================"
echo "  所有服务已停止"
echo "========================================"
echo -e "${NC}"
