# 批次推送并同步拉取接口文档
## 接口描述
`POST /api/v1/third-party/batch/push-sync` 用于第三方一次性推送批次和工单数据，服务端先完成批次入库，再同步拉取该请求涉及的工单预包装数据，最后直接在接口响应中返回每个工单的处理结果、错误原因和板件数量统计；该接口与现有 `POST /api/v1/third-party/batch/push`（仅入库+标记待拉取）并存。
## 前提条件
- 调用方已具备访问本服务的网络权限。
- 请求体必须是 `application/json`。
- 请求中的 `batchNum`、`batchType`、`optimizingFiles[].optimizingFileName`、`optimizingFiles[].workOrders[].workId` 必须提供。
- 数据库唯一约束已按当前版本生效（如 `uk_batch_work`、`uk_batch_work_box`、`uk_batch_work_box_package`、`uk_part_code`）。
## 使用限制
- 同一 `batchNum + workId` 同时只允许一个执行者拉取（`/push-sync` 与定时任务共享同一并发控制）。
- 若工单处于 `UPDATING`，本次同步接口对该工单返回 `PROCESSING`，不等待另一个执行者结束。
- 接口返回为批次级汇总 + 工单级明细，调用方需按 `workOrders[].status` 判断单工单成败。
- 仅在新增插入路径做重复校验；更新路径按覆盖语义处理，不额外做重复前置校验。

## 接口说明
| 步骤 | 说明 |
| -- | -- |
| 入库阶段 | 复用批次推送入库逻辑，支持同批次多次推送不同优化文件和工单。 |
| 同步阶段 | 仅同步本次请求中涉及的工单，不扫描全库。 |
| 并发控制 | `mes_work_order` 状态从 `NOT_PULLED` 原子更新为 `UPDATING` 成功才真正执行拉取。 |
| 共存模式 | 定时任务仍可按 `NOT_PULLED` 拉取，`push-sync` 只是新增同步入口。 |
| 单工单结果 | `PULLED` 计入成功；`NO_DATA`、`FAILED` 计入失败；`PROCESSING` 计入处理中。 |
| 错误映射 | 唯一约束冲突映射为明确中文错误，写入 `errorCode` 与 `errorMessage`。 |
## 请求
### 基本信息
| 项目 | 内容 |
| -- | -- |
| HTTP URL | /api/v1/third-party/batch/push-sync |
| HTTP Method | POST |
| Content-Type | application/json |
| 性能上限 | 代码未内置固定TPS限制，建议按压测结果在网关限流 |
### 请求头
| 名称 | 类型 | 必填 | 描述 |
| -- | -- | -- | -- |
| Content-Type | string | 是 | 固定为 `application/json` |
| Authorization | string | 否 | 若网关或上游鉴权启用则按环境传入 |
### 路径参数
| 名称 | 类型 | 必填 | 描述 |
| -- | -- | -- | -- |
| 无 | 无 | 无 | 无 |
### 查询参数
| 名称 | 类型 | 必填 | 描述 |
| -- | -- | -- | -- |
| 无 | 无 | 无 | 无 |
### 请求体
| 名称 | 类型 | 必填 | 说明 |
| -- | -- | -- | -- |
| batchNum | string | 是 | 批次号 |
| batchType | string | 是 | 批次类型（1衣柜柜体/2橱柜柜体/3衣柜门板/4橱柜门板/5合并条码/6补板） |
| productTime | string | 否 | 生产日期 |
| simpleBatchNum | string | 否 | 简易批次号 |
| nestingTime | string | 否 | 开料排样时间，兼容别名 `NestingTime` |
| ymba014 | string | 否 | 线路区域信息 |
| ymba016 | string | 否 | 属性标识 |
| optimizingFiles | array | 是 | 优化文件列表，兼容别名 `optimizingFileList` |
**optimizingFiles 子对象**
| 名称 | 类型 | 必填 | 说明 |
| -- | -- | -- | -- |
| optimizingFileName | string | 是 | 优化文件名称 |
| stationCode | string | 否 | 工位编码，兼容别名 `station` |
| urgency | integer | 否 | 是否加急（0不加急/1加急） |
| workOrders | array | 是 | 工单列表，兼容别名 `workOrderList` |
**workOrders 子对象**
| 名称 | 类型 | 必填 | 说明 |
| -- | -- | -- | -- |
| workId | string | 是 | 工单号 |
| route | string | 否 | 线路 |
| routeId | string | 否 | 线路ID，兼容别名 `routeid` |
| orderType | string | 否 | 订单类型 |
| deliveryTime | string | 否 | 交付日期，兼容别名 `DeliveryTime` |
| nestingTime | string | 否 | 开料排样时间，兼容别名 `NestingTime` |
| ymba014 | string | 否 | 线路区域信息 |
| ymba015 | string | 否 | 工位区域信息 |
| ymba016 | string | 否 | 属性标识 |
| part0 | string | 否 | 部件字段 |
| condition0 | string | 否 | 条件字段 |
| partTime0 | string | 否 | 部件时间字段 |
| zuz | integer | 否 | 组套标记 |
### 请求体示例
**场景1：正常推送并同步拉取**
```json
{
  "batchNum": "PCJH-260506-0001",
  "batchType": "3",
  "productTime": "2026-05-06",
  "simpleBatchNum": "26050601",
  "nestingTime": "2026-05-06 10:00:00",
  "ymba014": "A01",
  "ymba016": "NORMAL",
  "optimizingFiles": [
    {
      "optimizingFileName": "OPT-260506-A.txt",
      "stationCode": "C1A001",
      "urgency": 0,
      "workOrders": [
        {
          "workId": "WD000657014CBCP124",
          "route": "/",
          "routeId": "R001",
          "orderType": "N04"
        }
      ]
    }
  ]
}
```
**场景2：同批次增量补推多个工单**
```json
{
  "batchNum": "PCJH-260506-0001",
  "batchType": "3",
  "optimizingFiles": [
    {
      "optimizingFileName": "OPT-260506-B.txt",
      "workOrders": [
        {
          "workId": "WD000657014CBCP125"
        },
        {
          "workId": "WD000657014CBCP126"
        }
      ]
    }
  ]
}
```
### cURL 请求示例
```bash
curl -X POST "http://127.0.0.1:8080/api/v1/third-party/batch/push-sync" \
  -H "Content-Type: application/json" \
  -d '{
    "batchNum": "PCJH-260506-0001",
    "batchType": "3",
    "optimizingFiles": [
      {
        "optimizingFileName": "OPT-260506-A.txt",
        "workOrders": [
          { "workId": "WD000657014CBCP124" }
        ]
      }
    ]
  }'
```
## 响应
### 响应体
| 字段名 | 类型 | 描述 |
| -- | -- | -- |
| success | boolean | 批次整体是否成功（仅当 failedCount=0 为 true） |
| message | string | 处理结果描述 |
| batchNum | string | 批次号 |
| totalWorkOrders | integer | 本次处理工单总数 |
| successCount | integer | 成功工单数（仅 `PULLED`） |
| failedCount | integer | 失败工单数 |
| processingCount | integer | 处理中工单数（并发占用返回） |
| totalBoardCount | long | 本次所有工单板件总数 |
| finishedAt | string | 处理完成时间（LocalDateTime） |
| workOrders | array | 工单结果明细 |
**workOrders 子对象**
| 字段名 | 类型 | 描述 |
| -- | -- | -- |
| workId | string | 工单号 |
| status | string | 工单状态：`PULLED` / `NO_DATA` / `FAILED` / `PROCESSING` |
| boardCount | long | 当前工单板件数量 |
| errorCode | string | 错误码（失败时返回） |
| errorMessage | string | 中文错误信息（失败时返回） |
### 响应体示例
**HTTP 200：存在部分失败**
```json
{
  "success": false,
  "message": "同步批次推送处理完成（存在失败项）",
  "batchNum": "PCJH-260506-0001",
  "totalWorkOrders": 3,
  "successCount": 1,
  "failedCount": 1,
  "processingCount": 1,
  "totalBoardCount": 38,
  "finishedAt": "2026-05-06T16:20:30",
  "workOrders": [
    {
      "workId": "WD000657014CBCP124",
      "status": "PULLED",
      "boardCount": 38,
      "errorCode": null,
      "errorMessage": null
    },
    {
      "workId": "WD000657014CBCP125",
      "status": "FAILED",
      "boardCount": 0,
      "errorCode": "DUP_BOX_CODE",
      "errorMessage": "箱码重复：同一批次同一工单下箱码已存在，无法重复新增"
    },
    {
      "workId": "WD000657014CBCP126",
      "status": "PROCESSING",
      "boardCount": 0,
      "errorCode": null,
      "errorMessage": null
    }
  ]
}
```
**HTTP 400：参数校验失败**
```json
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "message": "批次号不能为空; 优化文件列表不能为空",
  "fieldErrors": {
    "batchNum": "批次号不能为空",
    "optimizingFiles": "优化文件列表不能为空"
  }
}
```
**HTTP 500：控制层异常**
```json
{
  "success": false,
  "message": "同步批次推送失败: <具体异常信息>",
  "batchNum": "PCJH-260506-0001"
}
```
### 错误码
| HTTP状态码 | 错误码 | 描述 | 排查建议 |
| -- | -- | -- | -- |
| 200 | DUP_WORK_ORDER | 工单重复：同一批次下工单号已存在，无法重复新增 | 检查同批次是否重复新增同工单 |
| 200 | DUP_BOX_CODE | 箱码重复：同一批次同一工单下箱码已存在，无法重复新增 | 检查 boxCode 是否在同批次同工单重复 |
| 200 | DUP_PACKAGE_NO | 包件重复：同一批次同一工单同一箱码下包号已存在，无法重复新增 | 检查 packageNo 是否重复 |
| 200 | DUP_PART_CODE | 板件重复：板件编码已存在，无法重复新增 | 检查 partCode 是否与其他工单冲突 |
| 200 | DUPLICATE_DATA | 数据唯一性冲突：存在重复记录，新增失败 | 查看数据库唯一索引冲突详情 |
| 200 | WORK_ORDER_NOT_FOUND | 工单不存在：该批次下未找到工单 | 校验 batchNum 与 workId 对应关系 |
| 200 | NO_DATA | 工单无预包装数据 | 检查第三方接口该工单是否存在可下拉预包装数据 |
| 200 | SYNC_PULL_ERROR | 同步拉取失败 | 查看 `workOrders[].errorMessage` 与服务日志 |
| 400 | VALIDATION_ERROR | 请求参数校验失败 | 根据 `fieldErrors` 修正参数 |
| 500 | INTERNAL_SERVER_ERROR | 服务端异常 | 查看应用日志并联系维护人员 |
