#!/usr/bin/env bash
set -euo pipefail

# Rolling upgrade with auto rollback for mes-service1
# Usage:
#   sudo bash upgrade.sh /path/to/new.jar

if [[ "${EUID}" -ne 0 ]]; then
  echo "[ERROR] Please run as root: sudo bash $0 <new-jar-path>"
  exit 1
fi

if [[ $# -lt 1 ]]; then
  echo "[ERROR] Missing argument."
  echo "Usage: sudo bash $0 <new-jar-path>"
  exit 1
fi

NEW_JAR="$1"
APP_NAME="mes-service1"
APP_DIR="/opt/${APP_NAME}"
APP_JAR="${APP_DIR}/${APP_NAME}.jar"
BACKUP_DIR="${APP_DIR}/backup"
BIN_DIR="${APP_DIR}/bin"
ENV_FILE="/etc/${APP_NAME}/${APP_NAME}.env"
SERVICE_NAME="${APP_NAME}.service"

if [[ ! -f "${NEW_JAR}" ]]; then
  echo "[ERROR] New jar not found: ${NEW_JAR}"
  exit 1
fi

if [[ ! -f "${APP_JAR}" ]]; then
  echo "[ERROR] Current jar not found: ${APP_JAR}"
  echo "[HINT] Run one-click deploy first."
  exit 1
fi

mkdir -p "${BACKUP_DIR}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_JAR="${BACKUP_DIR}/${APP_NAME}-${TIMESTAMP}.jar"

# Read settings
SERVER_PORT="8080"
HEALTH_URL="http://127.0.0.1:${SERVER_PORT}/actuator/health"
if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  SERVER_PORT="${SERVER_PORT:-8080}"
  HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${SERVER_PORT}/actuator/health}"
fi

echo "[INFO] New jar:     ${NEW_JAR}"
echo "[INFO] Current jar: ${APP_JAR}"
echo "[INFO] Backup jar:  ${BACKUP_JAR}"
echo "[INFO] Health URL:  ${HEALTH_URL}"

# Backup current version
cp -f "${APP_JAR}" "${BACKUP_JAR}"
echo "[INFO] Backup created."

# Install new version
install -o mes -g mes -m 755 "${NEW_JAR}" "${APP_JAR}"
ln -sf "${APP_JAR}" "${APP_DIR}/app.jar"
echo "[INFO] New jar installed."

# Restart service
echo "[INFO] Restarting service..."
systemctl restart "${SERVICE_NAME}"

# Health check loop
if ! command -v curl >/dev/null 2>&1; then
  echo "[WARN] curl not found; skip health check."
  systemctl --no-pager --full status "${SERVICE_NAME}" || true
  exit 0
fi

SUCCESS=0
for i in {1..12}; do
  sleep 5
  CODE=$(curl -sS -o /tmp/${APP_NAME}-upgrade-health.json -w "%{http_code}" --max-time 5 "${HEALTH_URL}" || true)
  BODY=$(cat /tmp/${APP_NAME}-upgrade-health.json 2>/dev/null || true)
  if [[ "${CODE}" == "200" ]] && grep -q '"status"\s*:\s*"UP"' <<<"${BODY}"; then
    SUCCESS=1
    break
  fi
  echo "[INFO] Health check attempt ${i}/12 not ready yet (code=${CODE})."
done

if [[ "${SUCCESS}" == "1" ]]; then
  echo "[DONE] Upgrade successful."
  systemctl --no-pager --full status "${SERVICE_NAME}" || true
  exit 0
fi

echo "[ERROR] Upgrade failed health check. Rolling back..."

# Rollback
install -o mes -g mes -m 755 "${BACKUP_JAR}" "${APP_JAR}"
ln -sf "${APP_JAR}" "${APP_DIR}/app.jar"
systemctl restart "${SERVICE_NAME}"

echo "[INFO] Rolled back to ${BACKUP_JAR}"
systemctl --no-pager --full status "${SERVICE_NAME}" || true

# Send alert if available
if [[ -x "${BIN_DIR}/alert.sh" ]]; then
  "${BIN_DIR}/alert.sh" "upgrade failed and rolled back: $(basename "${NEW_JAR}")"
fi

exit 1
