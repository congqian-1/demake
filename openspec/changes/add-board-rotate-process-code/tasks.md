## 1. 数据结构与迁移

- [x] 1.1 为 `mes_part` 增加 `rotate`、`process_code` 字段，并更新 `/Users/quancong/Documents/project/tongzhou/mes/deploy/ha/init-mes.sql`
- [x] 1.2 新增对应增量迁移脚本，覆盖线上库加列场景
- [x] 1.3 更新 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/test/resources/sql/init-mes-h2.sql`，保持测试库结构一致

## 2. 第三方落库与覆盖保存

- [x] 2.1 修改第三方预包装 DTO、转换逻辑和板件实体映射，接收 `rotate`、`processCode`
- [x] 2.2 修改板件新增与复用更新逻辑，确保 `savePrePackageDataWithOverwrite` 会写入并刷新这两个字段
- [x] 2.3 校验逻辑删除板件复用后仍只暴露有效记录，不把已删除历史板件返回到查询结果

## 3. 接口返回与文档

- [x] 3.1 修改板件相关 DTO / summary / hierarchy 节点，为板件返回补齐 `rotate`、`processCode`
- [x] 3.2 修改 `/api/v1/production/part/{partCode}/work-order-and-batch`、`/api/v1/production/part/{partCode}/package`、`/api/v1/production/part/{partCode}/detail` 的返回组装逻辑
- [x] 3.3 更新 Swagger 注释或接口文档，说明板件新增字段

## 4. 验证与回归

- [x] 4.1 补充集成测试，覆盖第三方拉取后板件字段成功入库
- [x] 4.2 补充集成测试，覆盖覆盖保存后 `rotate`、`processCode` 被刷新且已删除板件仍不返回
- [x] 4.3 补充接口测试，覆盖三个查询接口都返回新增字段
- [x] 4.4 运行测试并修复失败项，确认变更满足 capability spec
