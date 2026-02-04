# 快速开始

## 目标
验证板件详情接口返回上层层级实体。

## 步骤
1. 启动 mes-service1
2. 调用接口：/api/v1/production/part/{partCode}/detail
3. 检查响应中包含：batch、optimizingFile、workOrder、prepackageOrder、box、package
4. 对缺失层级的板件验证对应字段为 null

## 示例
```bash
curl -X GET "http://localhost:8080/api/v1/production/part/PART-001/detail"
```
