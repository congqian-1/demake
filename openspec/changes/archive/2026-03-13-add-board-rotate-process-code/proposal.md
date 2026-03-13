## Why

当前板件数据模型缺少 `rotate`（旋转）和 `processCode`（工艺代码）两个业务字段，导致第三方预包装数据、板件查询接口和报工相关返回无法完整传递板件工艺信息。需要补齐数据表、落库流程和接口返回，避免前端继续依赖外部拼装或丢失板件工艺属性。

## What Changes

- 为板件数据模型新增 `rotate` 和 `processCode` 字段，并同步更新数据库初始化脚本与迁移 SQL。
- 修改第三方预包装数据入库与覆盖保存逻辑，确保新字段能够稳定写入板件表并在覆盖刷新时保留一致性。
- 修改所有返回板件实体的接口与 DTO，包括批次层级、包装层级、板件详情以及相关报工返回，使前端能够拿到这两个字段。
- 补充 Swagger / 接口文档与测试，覆盖字段落库、查询返回和覆盖刷新场景。

## Capabilities

### New Capabilities

### Modified Capabilities
- `batch-prepackage-sync`: 板件数据结构和相关查询/落库行为增加 `rotate`、`processCode` 字段返回与持久化要求。

## Impact

- 影响板件表 `mes_part` 及对应实体、Mapper、DTO、转换逻辑。
- 影响预包装第三方数据拉取、覆盖保存和板件复用逻辑。
- 影响接口 `/api/v1/production/part/{partCode}/work-order-and-batch`、`/api/v1/production/part/{partCode}/package`、`/api/v1/production/part/{partCode}/detail` 以及其他返回板件实体的接口。
- 影响部署初始化 SQL、增量迁移 SQL、测试数据库建表脚本与集成测试。
