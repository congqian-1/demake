## 1. 数据库结构调整

- [x] 1.1 在 `/Users/quancong/Documents/project/tongzhou/mes/deploy/ha/init-mes.sql` 的 `mes_prepackage_order` 增加 `type`、`fdd8` 字段
- [x] 1.2 在 `/Users/quancong/Documents/project/tongzhou/mes/deploy/ha/` 新增 MySQL 增量迁移脚本，给线上库补同名字段
- [x] 1.3 更新 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/test/resources/sql/init-mes-h2.sql`，保持测试表结构一致

## 2. 拉取与入库链路

- [x] 2.1 扩展 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/ThirdPartyPrepackageResponseDTO.java` 与 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/PrepackageDataDTO.java`，解析 `type`、`FDD8`
- [x] 2.2 更新 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/main/java/com/tongzhou/mes/service1/converter/ThirdPartyPrepackageMapper.java`，把 `type`、`FDD8` 映射到标准预包装 DTO
- [x] 2.3 更新 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesPrepackageOrder.java` 与 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/PrePackageServiceImpl.java`，确保首次保存和覆盖保存都会写入并刷新两个字段

## 3. 查询接口返回补齐

- [x] 3.1 在 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/PrepackageOrderSummary.java` 与 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/hierarchy/PrepackageOrderDTO.java` 增加 `type`、`fdd8`
- [x] 3.2 更新 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/BatchPackagingQueryServiceImpl.java` 与 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/PartQueryServiceImpl.java` 的预包装订单映射逻辑，确保相关查询接口都返回这两个字段
- [x] 3.3 更新受影响返回模型与控制器的 Swagger 注释，补充 `type`、`fdd8` 字段说明

## 4. 验证与回归

- [x] 4.1 更新 `/Users/quancong/Documents/project/tongzhou/mes/mes-service1/src/test/java/com/tongzhou/mes/service1/integration/MesIntegrationSpecTest.java` 及相关 mock JSON，覆盖 `type`、`FDD8` 入库场景
- [x] 4.2 在 `/api/v1/production/part/{partCode}/work-order-and-batch`、`/api/v1/production/part/{partCode}/package`、`/api/v1/production/part/{partCode}/detail` 增加断言，验证预包装订单节点返回 `type`、`fdd8`
- [x] 4.3 执行 `mvn -pl mes-service1 -DfailIfNoTests=false test` 并修复失败项，直到全部通过
