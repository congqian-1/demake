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
| batchType | string | 是 | 批次类型（1=衣柜柜体/2=橱柜柜体/3=衣柜门板/4=橱柜门板/5=合并条码/6=补板） |
| productTime | string | 是 | 生产日期（格式：`YYYY-MM-DD` 或 `YYYY-MM-DD HH:mm:ss.S`） |
| simpleBatchNum | string | 否 | 简易批次号 |
| NestingTime | string | 否 | 开料/排样时间 |
| ymba014 | string | 否 | 线路/区域信息 |
| ymba016 | string | 否 | 属性标识 |
| optimizingFileList | array | 是 | 优化文件列表 |

### optimizingFileList[] 字段
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| optimizingFileName | string | 是 | 优化文件名称 |
| station | string | 是 | 工位编码（C1A001/C1A002/CMA001/CMA002/YMA001/YMA002） |
| urgency | int | 否 | 是否加急（0=不加急/1=加急） |
| workOrderList | array | 是 | 工单列表 |

### workOrderList[] 字段
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| workId | string | 是 | 工单号 |
| route | string | 否 | 线路 |
| routeid | string | 否 | 线路ID |
| orderType | string | 否 | 订单类型 |
| DeliveryTime | string | 否 | 交付日期 |
| NestingTime | string | 否 | 开料/排样时间 |
| ymba014 | string | 否 | 线路/区域信息 |
| ymba015 | string | 否 | 工位/区域信息 |
| ymba016 | string | 否 | 属性标识 |
| part0 | string | 否 | 部件字段 |
| condition0 | string | 否 | 条件字段 |
| partTime0 | string | 否 | 部件时间字段 |
| zuz | int | 否 | 组/套标记 |

## 4. 请求示例
```json
{
  "simpleBatchNum": "C2GT26012706-20",
  "NestingTime": "2026-03-23",
  "ymba014": "云南线",
  "ymba016": "N",
  "optimizingFileList": [
    {
      "optimizingFileName": "C2GT26012706-20-Y015-1",
      "urgency": 1,
      "station": "CMA002",
      "workOrderList": [
        {
          "orderType": "N04",
          "ymba014": "四川双流线",
          "part0": "NULL",
          "ymba016": "N",
          "workId": "WD000642973IBCP132",
          "DeliveryTime": "2026-02-02",
          "condition0": "NULL",
          "NestingTime": "2026-01-31",
          "route": "/",
          "routeid": "",
          "partTime0": "NULL",
          "zuz": 0
        }
      ]
    }
  ],
  "batchNum": "PCJH-260125-0105",
  "productTime": "2026-01-27 00:00:00.0",
  "batchType": "1"
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
