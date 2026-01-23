#!/bin/bash

##############################################
# mes-service1 启动脚本
##############################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目配置
PROJECT_NAME="mes-service1"
PROJECT_DIR="/Users/quancong/Documents/project/tongzhou/mes"
SERVICE_DIR="${PROJECT_DIR}/mes-service1"
JAR_FILE="${SERVICE_DIR}/target/${PROJECT_NAME}-1.0.0-SNAPSHOT.jar"
PROFILE="local"  # 可选: local, dev, stg, pet, prd

# JVM 参数配置
JAVA_OPTS="-Xms512m -Xmx1024m"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxGCPauseMillis=200"
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="${JAVA_OPTS} -XX:HeapDumpPath=${HOME}/logs/${PROJECT_NAME}/heapdump.hprof"

# 日志目录
LOG_DIR="${HOME}/logs/${PROJECT_NAME}"
mkdir -p ${LOG_DIR}

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}启动 ${PROJECT_NAME}${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo -e "${RED}错误: 未找到 Java 环境，请先安装 JDK${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo -e "${YELLOW}Java 版本: ${JAVA_VERSION}${NC}"

# 检查 JAR 文件是否存在
if [ ! -f "${JAR_FILE}" ]; then
    echo -e "${YELLOW}JAR 文件不存在，开始构建项目...${NC}"
    cd ${PROJECT_DIR}
    
    # 先安装依赖模块
    echo -e "${YELLOW}1. 安装 mes-parent...${NC}"
    mvn clean install -pl mes-parent -am -Dmaven.install.skip=false -DskipTests
    
    echo -e "${YELLOW}2. 安装 mes-service1-api...${NC}"
    mvn clean install -pl mes-api/mes-service1-api -am -Dmaven.install.skip=false -DskipTests
    
    echo -e "${YELLOW}3. 构建 mes-service1...${NC}"
    mvn clean package -pl mes-service1 -am -DskipTests
    
    if [ ! -f "${JAR_FILE}" ]; then
        echo -e "${RED}构建失败，JAR 文件不存在${NC}"
        exit 1
    fi
    echo -e "${GREEN}构建成功！${NC}"
fi

# 检查端口是否被占用
PORT=$(grep "port:" ${SERVICE_DIR}/src/main/resources/bootstrap.yml | head -1 | awk '{print $2}')
if lsof -Pi :${PORT} -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${RED}错误: 端口 ${PORT} 已被占用${NC}"
    echo -e "${YELLOW}占用端口的进程:${NC}"
    lsof -i :${PORT}
    exit 1
fi

echo -e "${YELLOW}配置文件: ${PROFILE}${NC}"
echo -e "${YELLOW}端口号: ${PORT}${NC}"
echo -e "${YELLOW}JVM 参数: ${JAVA_OPTS}${NC}"
echo -e "${GREEN}开始启动服务...${NC}"

# 启动服务
cd ${SERVICE_DIR}
java ${JAVA_OPTS} \
    -Dspring.profiles.active=${PROFILE} \
    -jar ${JAR_FILE} \
    > ${LOG_DIR}/console.log 2>&1 &

PID=$!
echo ${PID} > ${LOG_DIR}/${PROJECT_NAME}.pid

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}服务启动成功！${NC}"
echo -e "${GREEN}PID: ${PID}${NC}"
echo -e "${GREEN}端口: ${PORT}${NC}"
echo -e "${GREEN}日志文件: ${LOG_DIR}/console.log${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${YELLOW}查看日志: tail -f ${LOG_DIR}/console.log${NC}"
echo -e "${YELLOW}停止服务: kill ${PID}${NC}"
