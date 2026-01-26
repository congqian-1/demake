# 第三方批次推送接口对接文档

## 1. 接口说明
第三方 MES 系统向本系统推送批次及工单数据。

## 2. 接口信息
- URL：`http://10.30.70.110:8080/api/v1/third-party/batch/push`
- Method：`POST`
- Content-Type：`application/json`
- 鉴权：无

## 3. 请求参数（JSON Body）

### 顶层字段
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| batchNum | string | 是 | 批次号（唯一） |
| batchType | int | 是 | 批次类型（1=衣柜柜体/2=橱柜柜体/3=衣柜门板/4=橱柜门板/5=合并条码/6=补板） |
| productTime | string | 是 | 生产日期（格式：`YYYY-MM-DD`） |
| simpleBatchNum | string | 否 | 简易批次号 |
| optimizingFiles | array | 是 | 优化文件列表 |

### optimizingFiles[] 字段
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| optimizingFileName | string | 是 | 优化文件名称 |
| stationCode | string | 是 | 工位编码（C1A001/C1A002/CMA001/CMA002/YMA001/YMA002） |
| urgency | int | 否 | 是否加急（0=不加急/1=加急） |
| workOrders | array | 是 | 工单列表 |

### workOrders[] 字段
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| workId | string | 是 | 工单号 |
| route | string | 是 | 线路 |
| orderType | string | 是 | 订单类型 |

## 4. 请求示例
```json
{
  "batchNum": "PCJH-260125-0002",
  "batchType": 1,
  "productTime": "2026-01-26",
  "simpleBatchNum": "PCJH-260125-0002",
  "optimizingFiles": [
    {
      "optimizingFileName": "OPT-PCJH-260125-0002.txt",
      "stationCode": "C1A001",
      "urgency": 0,
      "workOrders": [
        {
          "workId": "DDN000070531BCP006",
          "route": "LINE-A",
          "orderType": "STANDARD"
        }
      ]
    }
  ]
}
```

## 5. 成功响应
```json
{
  "success": true,
  "message": "批次推送成功",
  "batchNo": "PCJH-260125-0002",
  "workOrderCount": 1,
  "fileCount": 1
}
```

## 6. 失败响应
```json
{
  "success": false,
  "message": "批次推送失败: 错误原因",
  "batchNo": "PCJH-260125-0002"
}
```

## 7. 错误码说明
- 200：成功
- 500：服务端异常（参数错误/保存失败等）

## 8. 注意事项
- `batchNum` 必须唯一，重复推送可能导致保存失败或覆盖策略由服务端控制。
- `productTime` 必须为 `YYYY-MM-DD` 格式。
- `optimizingFiles`、`workOrders` 不能为空。
