#!/bin/bash

##############################################
# mes-service1 调试模式启动脚本
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

# 调试端口配置
DEBUG_PORT=5005

# JVM 参数配置
JAVA_OPTS="-Xms512m -Xmx1024m"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxGCPauseMillis=200"
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="${JAVA_OPTS} -XX:HeapDumpPath=${HOME}/logs/${PROJECT_NAME}/heapdump.hprof"

# 远程调试参数
DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"

# 日志目录
LOG_DIR="${HOME}/logs/${PROJECT_NAME}"
mkdir -p ${LOG_DIR}

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}调试模式启动 ${PROJECT_NAME}${NC}"
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

# 检查服务端口是否被占用
SERVICE_PORT=$(grep "port:" ${SERVICE_DIR}/src/main/resources/bootstrap.yml | head -1 | awk '{print $2}')
if lsof -Pi :${SERVICE_PORT} -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${RED}错误: 服务端口 ${SERVICE_PORT} 已被占用${NC}"
    echo -e "${YELLOW}占用端口的进程:${NC}"
    lsof -i :${SERVICE_PORT}
    exit 1
fi

# 检查调试端口是否被占用
if lsof -Pi :${DEBUG_PORT} -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${RED}错误: 调试端口 ${DEBUG_PORT} 已被占用${NC}"
    echo -e "${YELLOW}占用端口的进程:${NC}"
    lsof -i :${DEBUG_PORT}
    exit 1
fi

echo -e "${YELLOW}配置文件: ${PROFILE}${NC}"
echo -e "${YELLOW}服务端口: ${SERVICE_PORT}${NC}"
echo -e "${YELLOW}调试端口: ${DEBUG_PORT}${NC}"
echo -e "${YELLOW}JVM 参数: ${JAVA_OPTS} ${DEBUG_OPTS}${NC}"
echo -e "${GREEN}开始启动服务（调试模式）...${NC}"

# 启动服务
cd ${SERVICE_DIR}
java ${JAVA_OPTS} ${DEBUG_OPTS} \
    -Dspring.profiles.active=${PROFILE} \
    -jar ${JAR_FILE} \
    > ${LOG_DIR}/console.log 2>&1 &

PID=$!
echo ${PID} > ${LOG_DIR}/${PROJECT_NAME}.pid

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}服务启动成功（调试模式）！${NC}"
echo -e "${GREEN}PID: ${PID}${NC}"
echo -e "${GREEN}服务端口: ${SERVICE_PORT}${NC}"
echo -e "${GREEN}调试端口: ${DEBUG_PORT}${NC}"
echo -e "${GREEN}日志文件: ${LOG_DIR}/console.log${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}【IDEA 远程调试配置】${NC}"
echo -e "1. 打开 IDEA -> Run -> Edit Configurations"
echo -e "2. 点击 '+' -> Remote JVM Debug"
echo -e "3. 配置如下:"
echo -e "   - Name: mes-service1-debug"
echo -e "   - Host: localhost"
echo -e "   - Port: ${DEBUG_PORT}"
echo -e "   - Use module classpath: mes-service1"
echo -e "4. 点击 Debug 按钮即可开始调试"
echo ""
echo -e "${YELLOW}【VS Code 远程调试配置】${NC}"
echo -e "在 .vscode/launch.json 中添加:"
echo -e '{'
echo -e '  "type": "java",'
echo -e '  "name": "Debug mes-service1",'
echo -e '  "request": "attach",'
echo -e '  "hostName": "localhost",'
echo -e "  \"port\": ${DEBUG_PORT}"
echo -e '}'
echo ""
echo -e "${YELLOW}查看日志: tail -f ${LOG_DIR}/console.log${NC}"
echo -e "${YELLOW}停止服务: kill ${PID}${NC}"
