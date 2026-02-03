# PartQueryController & WorkReportController 接口文档

> 基于源码生成：
> - `mes-service1/src/main/java/com/tongzhou/mes/service1/controller/PartQueryController.java`
> - `mes-service1/src/main/java/com/tongzhou/mes/service1/controller/WorkReportController.java`

## 1. 公共说明

- Base Path
  - `/api/v1/production`
- 鉴权
  - 无
- Content-Type
  - `application/json`

---

## 2. 板件查询接口（PartQueryController）

### 2.1 查询工单与批次信息

- URL
  - `GET /api/v1/production/part/{partCode}/work-order-and-batch`

- Path 参数
  - `partCode` (string, 必填)：板件码

- 成功响应
  - HTTP 200
  - Body：`ResultBatchHierarchy`

```json
{
  "code": "0",
  "message": "OK",
  "data": {
    "batch": {
      "id": 10,
      "batchNum": "PCJH-260125-0002",
      "batchType": 1,
      "productTime": "2026-01-27T00:00:00",
      "simpleBatchNum": "C2GT26012706-20"
    },
    "optimizingFiles": [
      {
        "id": 100,
        "batchId": 10,
        "optimizingFileName": "C2GT26012706-20-Y015-1",
        "stationCode": "CMA002",
        "urgency": 0,
        "workOrders": [
          {
            "id": 1,
            "workId": "DDN000070531BCP006",
            "batchId": 10,
            "optimizingFileId": 100,
            "batchNum": "PCJH-260125-0002",
            "route": "/",
            "orderType": "N04",
            "prepackageStatus": "PULLED",
            "retryCount": 0,
            "lastPullTime": "2026-01-27T11:29:54",
            "errorMessage": null
          }
        ]
      }
    ]
  }
}
```

- 失败响应
  - 404 板件不存在
  - 409 工单数据更新中
  - 500 其他错误

```json
{
  "success": false,
  "message": "板件码不存在",
  "partCode": "PART-001"
}
```

```json
{
  "success": false,
  "message": "工单数据更新中，请稍后再试",
  "partCode": "PART-001",
  "workId": "DDN000070531BCP006"
}
```

---

### 2.2 查询包装数据

- URL
  - `GET /api/v1/production/part/{partCode}/package`

- Path 参数
  - `partCode` (string, 必填)：板件码

- 成功响应
  - HTTP 200
  - Body：`ResultPrepackageHierarchy`

```json
{
  "code": "0",
  "message": "OK",
  "data": {
    "prepackageOrder": {
      "id": 300,
      "workOrderId": 1,
      "batchId": 10,
      "batchNum": "PCJH-260125-0002",
      "workId": "DDN000070531BCP006",
      "orderNum": "DDN000070531",
      "consignor": "宜宾区家装展厅",
      "contractNo": "JP宜宾（家装全包）2500094",
      "workNum": "DDN000070531BCP006",
      "receiver": "狄婷婷",
      "phone": "13419258883",
      "shipBatch": null,
      "installAddress": "四川省宜宾市...",
      "customer": "谢春平",
      "boxes": [
        {
          "id": 200,
          "prepackageOrderId": 300,
          "batchNum": "PCJH-260125-0002",
          "workId": "DDN000070531BCP006",
          "boxCode": "2-EYRK",
          "building": "A",
          "house": "1-1",
          "room": "803",
          "setno": 1,
          "color": "暖白麻面",
          "packages": [
            {
              "id": 5000,
              "boxId": 200,
              "packageNo": 8,
              "length": 1000,
              "width": 700,
              "depth": 90,
              "weight": 31.282,
              "boxType": "1040*740*91",
              "parts": [
                {
                  "id": 1000,
                  "packageId": 5000,
                  "partCode": "DDN0000705311003",
                  "layer": 3,
                  "piece": 2,
                  "itemCode": "0-1AXO0Y",
                  "itemName": "封顶板侧辅助板",
                  "matName": "18mm暖白麻面",
                  "itemLength": 80,
                  "itemWidth": 562,
                  "itemDepth": 18,
                  "sortOrder": 13,
                  "standardList": "[{\"00041\":1,\"00311\":1}]",
                  "workReports": []
                }
              ]
            }
          ]
        }
      ]
    }
  }
}
```

- 失败响应
  - 404 板件不存在
  - 409 工单数据更新中
  - 500 其他错误

---

### 2.3 查询板件详细信息

- URL
  - `GET /api/v1/production/part/{partCode}/detail`

- Path 参数
  - `partCode` (string, 必填)：板件码

- 成功响应
  - HTTP 200
  - Body：`PartDetailResponse`

```json
{
  "id": 1000,
  "partCode": "DDN0000705311003",
  "batchNum": "PCJH-260125-0002",
  "workId": "DDN000070531BCP006",
  "boxId": 200,
  "packageId": 5000,
  "layer": 3,
  "piece": 2,
  "itemCode": "0-1AXO0Y",
  "itemName": "封顶板侧辅助板",
  "matName": "18mm暖白麻面",
  "itemLength": 80,
  "itemWidth": 562,
  "itemDepth": 18,
  "xAxis": 924,
  "yAxis": 0,
  "zAxis": 36,
  "sortOrder": 13,
  "standardList": [
    {"00041": 1, "00311": 1}
  ],
  "standardListRaw": "[{\"00041\":1,\"00311\":1}]",
  "isDeleted": 0,
  "createdTime": "2026-01-27T11:00:00",
  "updatedTime": "2026-01-27T11:29:54",
  "description": "封顶板侧辅助板",
  "color": "18mm暖白麻面"
}
```

- 失败响应
  - 404 板件不存在
  - 500 其他错误

---

## 3. 板件报工接口（WorkReportController）

### 3.1 提交板件报工

- URL
  - `POST /api/v1/production/work-report`

- 请求体（JSON）
  - `WorkReportRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| partCode | string | 是 | 板件码（唯一标识） |
| partStatus | string | 是 | 板件状态（如：待加工/加工中/已完成） |
| stationCode | string | 是 | 工位编码 |
| stationName | string | 否 | 工位名称 |
| operatorId | string | 否 | 操作工ID |
| operatorName | string | 否 | 操作工姓名 |
| isCompleted | int | 是 | 是否完成（0=未完成/1=已完成） |

- 请求示例
```json
{
  "partCode": "DDN0000705311003",
  "partStatus": "加工中",
  "stationCode": "CMA002",
  "stationName": "裁切工位",
  "operatorId": "U10001",
  "operatorName": "张三",
  "isCompleted": 0
}
```

- 成功响应
  - HTTP 200

```json
{
  "success": true,
  "message": "报工成功",
  "partCode": "DDN0000705311003",
  "timestamp": 1769500000000
}
```

- 失败响应
  - 404 板件不存在
  - 409 重复报工
  - 500 系统错误

```json
{
  "success": false,
  "error": "板件不存在",
  "message": "板件码不存在: DDN0000705311003"
}
```

```json
{
  "success": false,
  "error": "重复报工",
  "message": "该板件已在该工位报工"
}
```

```json
{
  "success": false,
  "error": "系统错误",
  "message": "报工处理失败: ..."
}
```

---

## 4. 说明与约束

- 所有时间字段为服务器返回的时间格式（示例使用 `YYYY-MM-DDTHH:mm:ss`）。
- 产线客户端无需鉴权。
- `partCode` 作为主要查询输入，若不存在将返回 404。
- 工单更新期间查询会返回 409。
