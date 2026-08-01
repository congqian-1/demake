# Service1模块

## 接口与运维文档

- [同步推送接口说明](./docs/batch-push-sync-api.md)

## 看板工序同步功能

### 功能说明

`GET /api/v1/production/part/{partCode}/work-order-and-batch` 接口在返回批次层级数据前，会自动同步该批次下所有工单的最新板件数据。

### 启用/关闭

通过 JVM 启动参数控制：

```bash
# 开启（默认关闭）
java -Dmes.panel.process.sync.enabled=true -jar mes-service1.jar

# 或在 application.yml 中配置
mes:
  panel:
    process:
      sync:
        enabled: true
```

### 工作原理

1. 查询板件码对应的批次号
2. 遍历批次下所有工单，调用 `PrePackageService.pullSingleWorkOrderForSync` 逐工单同步
3. 同步结果（成功/失败/错误原因）写入 `mes_panel_process_sync` 表，按 `(batch_num, work_id)` 唯一
4. 同一批次只同步一次（数据库去重，重启不丢失）
5. 同步结果通过响应 `sync` 字段返回前端

### 同步结果表

```sql
-- 查询某批次的同步结果
SELECT batch_num, work_id, sync_result, error_detail, synced_at
FROM mes_panel_process_sync
WHERE batch_num = 'xxx'
ORDER BY work_id;
```

### 板件不存在时的自动发现

如果本地数据库查不到该板件码，接口会先调用 MES `batchQuery` 反查批次号，触发全批次同步后再重试查询。

## 上线部署

```bash
# 1. 执行数据库迁移（仅首次）
mysql -u root -p mes < deploy/ha/migrate-20260801-panel-process-sync.sql

# 2. 开启同步功能：编辑 /etc/mes-service1/mes-service1.env，追加 -D 参数
JVM_OPTS="...原有参数... -Dmes.panel.process.sync.enabled=true"

# 3. 升级 jar 包
sudo bash /opt/mes-release/upgrade.sh /path/to/mes-service1-1.0.0-SNAPSHOT.jar

# 4. 重载并重启
sudo systemctl daemon-reload
sudo systemctl restart mes-service1
```
