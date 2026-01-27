#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"

BATCH_NUM="${BATCH_NUM:-PCJH-260125-0002}"
WORK_ID="${WORK_ID:-DDN000070531BCP006}"
PART_CODE="${PART_CODE:-}"

echo "Base URL: ${BASE_URL}"
echo "Batch Num: ${BATCH_NUM}"
echo "Work ID: ${WORK_ID}"
echo "Part Code: ${PART_CODE:-<empty>}"
echo ""

echo "==> Health"
curl -sS -i "${BASE_URL}/actuator/health"
echo ""

echo "==> Echo"
curl -sS -i "${BASE_URL}/api/v1/echo/hello?echo=ping"
echo ""

echo "==> Batch Push"
curl -sS -i -X POST "${BASE_URL}/api/v1/third-party/batch/push" \
  -H "Content-Type: application/json" \
  -d "{
    \"simpleBatchNum\": \"${BATCH_NUM}\",
    \"NestingTime\": \"$(date +%F)\",
    \"ymba014\": \"云南线\",
    \"ymba016\": \"N\",
    \"optimizingFileList\": [
      {
        \"optimizingFileName\": \"OPT-${BATCH_NUM}.txt\",
        \"urgency\": 1,
        \"station\": \"CMA002\",
        \"workOrderList\": [
          {
            \"orderType\": \"N04\",
            \"ymba014\": \"云南线\",
            \"part0\": \"NULL\",
            \"ymba016\": \"N\",
            \"workId\": \"${WORK_ID}\",
            \"DeliveryTime\": \"$(date +%F)\",
            \"condition0\": \"NULL\",
            \"NestingTime\": \"$(date +%F)\",
            \"route\": \"/\",
            \"routeid\": \"\",
            \"partTime0\": \"NULL\",
            \"zuz\": 0
          }
        ]
      }
    ],
    \"batchNum\": \"${BATCH_NUM}\",
    \"productTime\": \"$(date '+%F 00:00:00.0')\",
    \"batchType\": \"1\"
  }"
echo ""

echo "==> Work Order Repull"
curl -sS -i -X POST "${BASE_URL}/api/v1/admin/work-order/${WORK_ID}/repull" \
  -H "Content-Type: application/json" \
  -d '{
    "operator": "tester",
    "reason": "api-test"
  }'
echo ""

if [[ -z "$PART_CODE" ]]; then
  echo "==> Part Query + Work Report (SKIPPED: set PART_CODE to enable)"
else
  echo "==> Part Query: work-order-and-batch"
  curl -sS -i "${BASE_URL}/api/v1/production/part/${PART_CODE}/work-order-and-batch"
  echo ""

  echo "==> Part Query: package"
  curl -sS -i "${BASE_URL}/api/v1/production/part/${PART_CODE}/package"
  echo ""

  echo "==> Part Query: detail"
  curl -sS -i "${BASE_URL}/api/v1/production/part/${PART_CODE}/detail"
  echo ""

  echo "==> Work Report"
  curl -sS -i -X POST "${BASE_URL}/api/v1/production/work-report" \
    -H "Content-Type: application/json" \
    -d "{
      \"partCode\": \"${PART_CODE}\",
      \"partStatus\": \"DONE\",
      \"stationCode\": \"C1A001\",
      \"stationName\": \"Station-1\",
      \"operatorId\": \"op-1\",
      \"operatorName\": \"tester\",
      \"isCompleted\": 1
    }"
  echo ""
fi
