# PartQueryController & WorkReportController 接口文档

> 仅包含与“真实打包包号”相关的增量说明。

## 1. 板件报工接口（WorkReportController）

### 1.1 提交板件报工

- URL
  - `POST /api/v1/production/work-report`

- 请求体新增字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| realPackageNo | string | 否 | 真实打包包号（可空，空值不覆盖已有值） |

- 请求示例

```json
{
  "partCode": "PART-001",
  "partStatus": "DONE",
  "stationCode": "C1A001",
  "stationName": "开料",
  "operatorId": "OP-1",
  "operatorName": "测试员",
  "isCompleted": 1,
  "realPackageNo": "PKG-REAL-001"
}
```

## 2. 板件查询接口（PartQueryController）

### 2.1 查询工单与批次信息

- URL
  - `GET /api/v1/production/part/{partCode}/work-order-and-batch`
- 返回说明
  - 在层级返回的板件实体中新增 `realPackageNo`

### 2.2 查询包装数据

- URL
  - `GET /api/v1/production/part/{partCode}/package`
- 返回说明
  - 在层级返回的板件实体中新增 `realPackageNo`

### 2.3 查询板件详细信息

- URL
  - `GET /api/v1/production/part/{partCode}/detail`
- 返回说明
  - 响应字段新增 `realPackageNo`
