#!/bin/bash

##############################################
# mes-service1 停止脚本
##############################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PROJECT_NAME="mes-service1"
LOG_DIR="${HOME}/logs/${PROJECT_NAME}"
PID_FILE="${LOG_DIR}/${PROJECT_NAME}.pid"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}停止 ${PROJECT_NAME}${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查 PID 文件是否存在
if [ -f "${PID_FILE}" ]; then
    PID=$(cat ${PID_FILE})
    
    # 检查进程是否存在
    if ps -p ${PID} > /dev/null 2>&1; then
        echo -e "${YELLOW}正在停止进程 ${PID}...${NC}"
        kill ${PID}
        
        # 等待进程结束
        for i in {1..30}; do
            if ! ps -p ${PID} > /dev/null 2>&1; then
                echo -e "${GREEN}服务已停止${NC}"
                rm -f ${PID_FILE}
                exit 0
            fi
            sleep 1
            echo -n "."
        done
        
        # 如果进程仍未结束，强制杀死
        if ps -p ${PID} > /dev/null 2>&1; then
            echo -e "\n${YELLOW}进程未响应，强制停止...${NC}"
            kill -9 ${PID}
            rm -f ${PID_FILE}
            echo -e "${GREEN}服务已强制停止${NC}"
        fi
    else
        echo -e "${YELLOW}进程 ${PID} 不存在${NC}"
        rm -f ${PID_FILE}
    fi
else
    echo -e "${YELLOW}PID 文件不存在，尝试通过端口查找进程...${NC}"
    
    # 通过端口查找进程
    PORT=6003
    PID=$(lsof -ti:${PORT})
    
    if [ ! -z "${PID}" ]; then
        echo -e "${YELLOW}找到占用端口 ${PORT} 的进程: ${PID}${NC}"
        kill ${PID}
        echo -e "${GREEN}服务已停止${NC}"
    else
        echo -e "${YELLOW}未找到运行中的服务${NC}"
    fi
fi
