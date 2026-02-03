# 任务清单：板件真实包号

**输入**：来自 `/specs/003-real-package-no/` 的设计文档
**前置条件**：plan.md、spec.md、research.md、data-model.md、contracts/

**测试**：包含接口与服务集成测试任务（规格已要求）

**组织方式**：按用户故事分组，确保每个故事可独立实现与测试。

## 阶段 1：基础变更（共享前置）

- [ ] T001 更新板件表结构，新增真实包号字段：`deploy/ha/init-mes.sql`
- [ ] T002 新增迁移脚本以兼容存量库：`deploy/ha/migrate-YYYYMMDD-real-package-no.sql`
- [ ] T003 [P] 更新板件实体字段：`mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesBoard.java`
- [ ] T004 [P] 更新板件详情响应字段：`mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/PartDetailResponse.java`
- [ ] T005 [P] 更新层级查询板件 DTO 字段：`mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/hierarchy/PartDTO.java`

**检查点**：实体与 DTO 均可表达真实包号字段

---

## 阶段 2：用户故事 1 - 报工记录真实包号（优先级：P1）🎯

**目标**：报工提交可写入真实包号并保存到板件记录

**独立测试**：提交报工携带真实包号后，从板件详情中读取并验证

### 用户故事 1 的测试

- [ ] T006 [P] [US1] 补充报工与详情验证集成测试：`mes-service1/src/test/java/com/tongzhou/mes/service1/integration/MesIntegrationSpecTest.java`

### 用户故事 1 的实现

- [ ] T007 [P] [US1] 增加报工入参字段：`mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/WorkReportRequest.java`
- [ ] T008 [US1] 报工保存时写入板件真实包号字段（含空值不覆盖规则）：`mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/WorkReportServiceImpl.java`

**检查点**：报工写入真实包号且空值不覆盖

---

## 阶段 3：用户故事 2 - 查询返回真实包号（优先级：P2）

**目标**：查询批次层级、包装层级与板件详情均返回真实包号

**独立测试**：三类查询返回结构中均包含真实包号字段

### 用户故事 2 的测试

- [ ] T009 [P] [US2] 更新层级查询返回断言：`mes-service1/src/test/java/com/tongzhou/mes/service1/integration/MesIntegrationSpecTest.java`

### 用户故事 2 的实现

- [ ] T010 [US2] 批次层级板件映射补充真实包号：`mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/BatchPackagingQueryServiceImpl.java`
- [ ] T011 [US2] 板件详情查询补充真实包号赋值：`mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/PartQueryServiceImpl.java`

**检查点**：三类查询均可返回真实包号字段

---

## 阶段 4：用户故事 3 - 空值与覆盖规则一致（优先级：P3）

**目标**：统一覆盖规则，避免空值误覆盖

**独立测试**：空值报工不覆盖，非空值覆盖

### 用户故事 3 的测试

- [ ] T012 [P] [US3] 增加空值与覆盖规则测试：`mes-service1/src/test/java/com/tongzhou/mes/service1/integration/MesIntegrationSpecTest.java`

### 用户故事 3 的实现

- [ ] T013 [US3] 报工服务内实现空值不覆盖逻辑（如需要与 T008 合并）：`mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/WorkReportServiceImpl.java`

**检查点**：覆盖规则一致且通过测试

---

## 阶段 5：文档与契约同步

- [ ] T014 [P] 更新接口文档示例与字段说明：`docs/part-and-work-report-api.md`
- [ ] T015 [P] 校验契约文件与实现一致：`specs/003-real-package-no/contracts/part-real-package-no.yaml`
- [ ] T016 验证 quickstart 文档步骤可执行：`specs/003-real-package-no/quickstart.md`

---

## 依赖关系与执行顺序

- 阶段 1 完成后才开始用户故事任务
- 用户故事 1 与 2 可并行推进（共享实体/DTO 已完成）
- 用户故事 3 依赖用户故事 1 的报工写入逻辑
- 文档同步在实现完成后执行
