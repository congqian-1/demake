# Tasks: MES 系统对接集成

**Feature Branch**: `001-mes-integration`  
**Input**: Design documents from `/specs/001-mes-integration/`  
**Prerequisites**: plan.md (技术方案), spec.md (用户故事)

**Organization**: 任务按用户故事分组，支持独立实现和测试

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 任务所属用户故事（US1, US2等）
- 所有任务包含具体文件路径

## Path Conventions

项目结构：
- `mes-service1/src/main/java/com/tongzhou/mes/service1/` - 业务代码
- `mes-service1/src/main/resources/` - 配置文件
- `mes-service1/src/test/java/` - 测试代码

---

## Phase 1: Setup (项目初始化)

**Purpose**: 数据库设计和项目配置初始化

- [X] T001 创建数据库DDL脚本：10张表（批次、优化文件、工单、预包装订单、箱码、包件、板件、报工记录、修正日志、邮件配置） in scripts/ddl/001-create-tables.sql
- [X] T002 创建索引和外键约束脚本 in scripts/ddl/002-create-indexes.sql
- [X] T003 创建初始化数据脚本（邮件通知配置表） in scripts/ddl/003-init-data.sql
- [X] T004 [P] 添加Maven依赖：spring-boot-starter-mail in mes-service1/pom.xml
- [X] T005 [P] 添加Maven依赖：spring-retry in mes-service1/pom.xml
- [X] T006 [P] 配置application.yml：邮件服务（QQ邮箱SMTP）、第三方MES API地址、MyBatis-Plus逻辑删除 in mes-service1/src/main/resources/application.yml

---

## Phase 2: Foundational (基础设施 - 阻塞所有用户故事)

**Purpose**: 核心基础设施，必须在所有用户故事之前完成

**⚠️ CRITICAL**: 所有用户故事依赖此阶段完成

- [X] T007 [P] 创建Entity：Batch（批次表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesBatch.java
- [X] T008 [P] 创建Entity：OptimizingFile（优化文件表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesOptimizationFile.java
- [X] T009 [P] 创建Entity：WorkOrder（工单表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesWorkOrder.java
- [X] T010 [P] 创建Entity：PrePackageOrder（预包装订单表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesPrepackageOrder.java
- [X] T011 [P] 创建Entity：Box（箱码表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesBoxCode.java
- [X] T012 [P] 创建Entity：Package（包件表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesPackage.java
- [X] T013 [P] 创建Entity：Part（板件表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesBoard.java
- [X] T014 [P] 创建Entity：WorkReport（报工记录表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesWorkReport.java
- [X] T015 [P] 创建Entity：WorkOrderCorrectionLog（修正日志表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesCorrectionLog.java
- [X] T016 [P] 创建Entity：EmailNotificationConfig（邮件配置表） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/entity/MesEmailConfig.java
- [X] T017 [P] 创建Mapper：BatchMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesBatchMapper.java
- [X] T018 [P] 创建Mapper：OptimizingFileMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesOptimizationFileMapper.java
- [X] T019 [P] 创建Mapper：WorkOrderMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesWorkOrderMapper.java
- [X] T020 [P] 创建Mapper：PrePackageOrderMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesPrepackageOrderMapper.java
- [X] T021 [P] 创建Mapper：BoxMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesBoxCodeMapper.java
- [X] T022 [P] 创建Mapper：PackageMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesPackageMapper.java
- [X] T023 [P] 创建Mapper：PartMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesBoardMapper.java
- [X] T024 [P] 创建Mapper：WorkReportMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesWorkReportMapper.java
- [X] T025 [P] 创建Mapper：WorkOrderCorrectionLogMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesCorrectionLogMapper.java
- [X] T026 [P] 创建Mapper：EmailNotificationConfigMapper in mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesEmailConfigMapper.java
- [X] T027 [P] 创建通用响应DTO：BatchPushDTO in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/BatchPushDTO.java
- [X] T028 创建第三方MES API客户端：ThirdPartyMesClient（使用OkHttp，实现getPrePackageInfo方法） in mes-service1/src/main/java/com/tongzhou/mes/service1/client/ThirdPartyMesClient.java
- [X] T029 创建第三方API响应DTO：PrepackageDataDTO（含嵌套结构：PrePackageInfo、BoxInfoDetail、PartInfo） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/PrepackageDataDTO.java

**Checkpoint**: 基础设施就绪 - 用户故事实现可以并行开始

---

## Phase 3: User Story 1 - 批次与工单数据接收 (Priority: P1) 🎯 MVP

**Goal**: 第三方MES系统推送批次及工单数据到我方系统，支持批次号、批次类型、优化文件列表、工单信息的完整接收和存储

**Independent Test**: 模拟第三方MES调用批次推送接口，验证批次和工单数据完整入库，批次号唯一性，幂等性处理（重复推送更新现有数据）

### Implementation for User Story 1

- [X] T030 [P] [US1] 创建Request DTO：BatchPushRequest（含嵌套类OptimizingFileInfo、WorkOrderInfo） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/BatchPushRequest.java
- [X] T031 [P] [US1] 创建Converter：BatchConverter（Entity与DTO互转） in mes-service1/src/main/java/com/tongzhou/mes/service1/converter/BatchConverter.java
- [X] T032 [US1] 实现Service：BatchService.saveBatch()（批次+优化文件+工单三层保存，幂等性处理：检查批次号，若存在则删除旧工单和优化文件后重新插入） in mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/BatchServiceImpl.java
- [X] T033 [US1] 实现Controller：BatchController.pushBatch()（POST /api/v1/third-party/batch/push，参数校验，异常处理） in mes-service1/src/main/java/com/tongzhou/mes/service1/controller/BatchController.java
- [X] T034 [US1] 添加日志记录：批次推送接口调用（记录批次号、工单数量、处理结果） in BatchServiceImpl和BatchController
- [ ] T035 [US1] 添加单元测试：批次推送成功场景（5个验收场景：包含2个工单的批次、参数校验失败、重复推送幂等性、字段一致性、加急标识） in mes-service1/src/test/java/com/tongzhou/mes/service1/service/BatchServiceTest.java

**Checkpoint**: 批次推送接口完整实现，可独立测试和演示

---

## Phase 4: User Story 2 - 预包装数据自动拉取 (Priority: P1)

**Goal**: 定时任务每1秒扫描"未拉取"工单，调用第三方MES接口获取预包装数据，支持指数退避重试（1s/2s/4s）、邮件通知、状态管理

**Independent Test**: 在数据库插入"未拉取"状态工单，启动定时任务，验证1秒内调用第三方接口并成功拉取数据入库，工单状态更新为"已拉取"；模拟接口失败验证重试和邮件通知

### Implementation for User Story 2

- [X] T036 [P] [US2] 实现EmailNotificationService（发送预包装拉取失败通知邮件，包含批次号、工单号、失败原因、重试次数） in mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/EmailNotificationServiceImpl.java
- [X] T037 [US2] 实现PrePackageService.pullPendingWorkOrders()（查询"未拉取"工单列表，每次处理50个上限） in mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/PrePackageServiceImpl.java
- [X] T038 [US2] 实现PrePackageService.pullSingleWorkOrder()（单个工单拉取逻辑，更新状态为"拉取中"→"已拉取"，调用pullWithRetry） in PrePackageServiceImpl
- [X] T039 [US2] 实现PrePackageService.pullWithRetry()（指数退避重试：1s/2s/4s间隔，最多3次，失败后抛异常） in PrePackageServiceImpl
- [X] T040 [US2] 实现PrePackageService.savePrePackageData()（四层嵌套保存：订单→箱码→包件→板件，冗余batch_num字段） in PrePackageServiceImpl
- [X] T041 [US2] 实现PrePackageService.handlePullFailure()（更新重试次数，3次失败后标记"拉取失败"，发送邮件通知） in PrePackageServiceImpl
- [X] T042 [US2] 实现定时任务：PrePackagePullTask（@Scheduled fixedDelay=1000ms，使用AtomicBoolean实现互斥机制，避免并发执行） in mes-service1/src/main/java/com/tongzhou/mes/service1/scheduled/PrePackagePullTask.java
- [X] T043 [US2] 添加日志记录：预包装数据拉取（记录工单号、接口调用时间、响应状态、重试次数、错误消息） in PrePackageServiceImpl和PrePackagePullTask
- [ ] T044 [US2] 添加集成测试：预包装拉取成功、无数据、重试失败、邮件通知、定时任务互斥（6个验收场景） in mes-service1/src/test/java/com/tongzhou/mes/service1/service/PrePackageServiceTest.java

**Checkpoint**: 预包装自动拉取功能完整实现，可独立测试（与Story 1组合测试）

---

## Phase 5: User Story 3 - 板件码查询工单与批次信息 (Priority: P2)

**Goal**: 产线客户端通过板件码查询该板件所属工单的全部字段和批次的全部字段（支持优化文件信息）

**Independent Test**: 准备完整的批次-优化文件-工单-预包装-板件数据链，调用查询接口，验证返回工单、优化文件和批次信息完整且准确

### Implementation for User Story 3

- [ ] T045 [P] [US3] 创建Response DTO：PartWorkOrderBatchResponse（含WorkOrderInfo、OptimizingFileInfo、BatchInfo三个嵌套类） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/PartWorkOrderBatchResponse.java
- [ ] T046 [US3] 实现Service：PartQueryService.queryWorkOrderAndBatch()（查询板件→工单→优化文件→批次，检查工单状态是否为"更新中"，若是则抛WorkOrderUpdatingException） in mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/PartQueryServiceImpl.java
- [ ] T047 [US3] 创建自定义异常：WorkOrderUpdatingException（HTTP 409，提示"工单数据更新中，请稍后重试"） in mes-service1/src/main/java/com/tongzhou/mes/service1/exception/WorkOrderUpdatingException.java
- [ ] T048 [US3] 创建自定义异常：PartNotFoundException（板件码不存在） in mes-service1/src/main/java/com/tongzhou/mes/service1/exception/PartNotFoundException.java
- [ ] T049 [US3] 实现Controller：PartQueryController.queryWorkOrderAndBatch()（GET /api/v1/production/part/{partCode}/work-order-and-batch，异常处理返回HTTP 409或错误提示） in mes-service1/src/main/java/com/tongzhou/mes/service1/controller/PartQueryController.java
- [ ] T050 [US3] 添加单元测试：查询成功、板件码不存在、工单更新中（3个验收场景） in mes-service1/src/test/java/com/tongzhou/mes/service1/service/PartQueryServiceTest.java

**Checkpoint**: 板件码查询工单批次信息功能完整，可独立测试

---

## Phase 6: User Story 4 - 板件码查询包装数据 (Priority: P2)

**Goal**: 产线客户端通过板件码查询包装结构信息（订单→箱码→包件→板件，含所有层级字段和standardList）

**Independent Test**: 准备完整预包装数据结构，调用查询接口，验证箱码信息、层级、分拣顺序、标准码集合完整返回

### Implementation for User Story 4

- [ ] T051 [P] [US4] 创建Response DTO：PartPackageResponse（含BoxInfo、OrderInfo、PositionInfo，BoxInfo包含partList） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/PartPackageResponse.java
- [ ] T052 [US4] 实现Service：PartQueryService.queryPackage()（查询板件→包件→箱码→订单，返回包装结构和板件位置，检查工单状态） in PartQueryServiceImpl
- [ ] T053 [US4] 实现Controller：PartQueryController.queryPackage()（GET /api/v1/production/part/{partCode}/package，异常处理） in PartQueryController
- [ ] T054 [US4] 添加单元测试：查询包装数据成功、嵌套结构完整、箱内多板件、standardList正确返回（4个验收场景） in PartQueryServiceTest.java

**Checkpoint**: 板件码查询包装数据功能完整，可独立测试

---

## Phase 7: User Story 5 - 板件码查询板件详细信息 (Priority: P2)

**Goal**: 产线客户端通过板件码查询板件自身详细信息（板件ID、描述、花色、尺寸、坐标、分拣顺序等全部字段）

**Independent Test**: 准备板件数据记录，调用查询接口，验证返回板件属性完整且准确

### Implementation for User Story 5

- [ ] T055 [P] [US5] 创建Response DTO：PartDetailResponse（板件全部字段，含standardList解析后的List<Map<String, Integer>>） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/PartDetailResponse.java
- [ ] T056 [US5] 实现Service：PartQueryService.queryDetail()（查询板件详细信息，is_deleted=0过滤） in PartQueryServiceImpl
- [ ] T057 [US5] 实现Controller：PartQueryController.queryDetail()（GET /api/v1/production/part/{partCode}/detail，异常处理） in PartQueryController
- [ ] T058 [US5] 添加单元测试：查询板件详细信息成功、部分字段为空、板件码不存在（3个验收场景） in PartQueryServiceTest.java

**Checkpoint**: 板件码查询详细信息功能完整，三个查询接口可组合测试

---

## Phase 8: User Story 6 - 板件报工 (Priority: P3)

**Goal**: 产线客户端提交板件报工记录（板件码、状态、工位），系统记录生产轨迹，支持幂等性（状态转换去重）

**Independent Test**: 模拟产线客户端提交报工记录，验证系统正确记录并可查询；提交相同状态重复报工验证幂等性拒绝

### Implementation for User Story 6

- [ ] T059 [P] [US6] 创建Request DTO：WorkReportRequest（partCode、partStatus、stationCode、stationName） in mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/WorkReportRequest.java
- [ ] T060 [US6] 实现Service：WorkReportService.saveWorkReport()（查询板件冗余work_id，幂等性检查：查询最后一次报工，状态相同则抛DuplicateWorkReportException） in mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/WorkReportServiceImpl.java
- [ ] T061 [US6] 创建自定义异常：DuplicateWorkReportException（重复报工） in mes-service1/src/main/java/com/tongzhou/mes/service1/exception/DuplicateWorkReportException.java
- [ ] T062 [US6] 实现Controller：WorkReportController.submitWorkReport()（POST /api/v1/production/work-report，参数校验，异常处理） in mes-service1/src/main/java/com/tongzhou/mes/service1/controller/WorkReportController.java
- [ ] T063 [US6] 添加日志记录：报工提交（记录板件码、工位编码、状态、报工时间） in WorkReportServiceImpl
- [ ] T064 [US6] 添加单元测试：报工成功、多次报工、参数校验失败、板件码不存在、幂等性（4个验收场景） in mes-service1/src/test/java/com/tongzhou/mes/service1/service/WorkReportServiceTest.java

**Checkpoint**: 板件报工功能完整，可独立测试

---

## Phase 9: User Story 7 - 工单数据修正与重新拉取 (Priority: P2)

**Goal**: 管理员重置工单状态为"未拉取"，定时任务重新拉取预包装数据，覆盖订单/箱码/包件/板件，但保留报工记录（软删除板件，保留报工关联）

**Independent Test**: 准备已拉取预包装且有报工记录的工单，手动重置状态为"未拉取"，启动定时任务，验证预包装数据和板件基础数据已更新，报工记录完整保留

### Implementation for User Story 7

- [ ] T065 [US7] 实现PrePackageService.repullAndUpdate()（记录修正前数据，重置工单状态为"未拉取"，记录修正日志） in PrePackageServiceImpl
- [ ] T066 [US7] 实现PrePackageService.savePrePackageDataWithOverwrite()（覆盖逻辑：软删除旧板件is_deleted=1，物理删除旧包件/箱码/订单，插入新数据） in PrePackageServiceImpl
- [ ] T067 [US7] 修改PrePackageService.pullSingleWorkOrder()（拉取前检查是否为重新拉取，若是则调用savePrePackageDataWithOverwrite，拉取前设置状态为"更新中"UPDATING，完成后设置为"已拉取"） in PrePackageServiceImpl
- [ ] T068 [US7] 修改PartQueryService所有查询方法（添加工单状态检查，若为"更新中"则抛WorkOrderUpdatingException返回HTTP 409） in PartQueryServiceImpl
- [ ] T069 [US7] 添加日志记录：工单数据修正（记录操作人、操作时间、工单号、修正原因、板件数量变化、修正结果） in PrePackageServiceImpl
- [ ] T070 [US7] 添加集成测试：工单修正后重新拉取、预包装数据覆盖、报工记录保留、板件数量变化处理、并发查询返回409、重新拉取中报工不受影响、多工单同时修正（7个验收场景） in mes-service1/src/test/java/com/tongzhou/mes/service1/service/WorkOrderCorrectionTest.java

**Checkpoint**: 工单数据修正与重新拉取功能完整，可独立测试（需结合Story 2和Story 6）

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: 跨用户故事的完善和优化

- [ ] T071 [P] 添加API文档注解：所有Controller使用SpringDoc的@Tag、@Operation、@ApiModel、@ApiModelProperty in 所有Controller和DTO类
- [ ] T072 [P] 完善异常处理：全局异常处理器（@RestControllerAdvice），统一返回StandardResponse in mes-service1/src/main/java/com/tongzhou/mes/service1/exception/GlobalExceptionHandler.java
- [ ] T073 [P] 性能监控配置：Spring Actuator健康检查、定时任务状态监控、接口响应时间指标 in application.yml
- [ ] T074 代码Review与重构：检查所有Service事务边界、Mapper SQL优化、冗余代码清理
- [ ] T075 [P] 编写部署文档：环境变量配置（MES_THIRD_PARTY_BASE_URL、MAIL_AUTH_CODE）、数据库初始化步骤、健康检查命令 in docs/deployment.md
- [ ] T076 [P] 编写运维文档：失败工单SQL查询、邮件通知配置更新、定时任务监控方法 in docs/operations.md
- [ ] T077 边界场景测试：批次推送网络异常、预包装拉取失败、定时任务长时间运行互斥、板件码不存在、报工幂等性、系统重启恢复、并发冲突、板件数量变化（11个边界场景） in mes-service1/src/test/java/com/tongzhou/mes/service1/integration/EdgeCaseTest.java
- [ ] T078 性能测试：批次推送响应时间≤500ms（100/s并发）、定时任务吞吐量≥50工单/秒、产线查询响应时间90%≤200ms（500/s并发）、报工接口≤100ms（200/s并发） in mes-service1/src/test/jmeter/performance-test.jmx

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 - 可立即开始
- **Foundational (Phase 2)**: 依赖Setup完成 - **阻塞所有用户故事**
- **User Stories (Phase 3-9)**: 所有依赖Foundational完成
  - 用户故事可并行（如有多人开发）
  - 或按优先级顺序（P1 → P2 → P3）
- **Polish (Phase 10)**: 依赖所有用户故事完成

### User Story Dependencies

- **User Story 1 (P1)**: 依赖Foundational - 无其他故事依赖
- **User Story 2 (P1)**: 依赖Foundational和US1 - US2需要US1的批次和工单数据
- **User Story 3 (P2)**: 依赖Foundational和US2 - US3需要US2的预包装数据
- **User Story 4 (P2)**: 依赖Foundational和US2 - US4需要US2的预包装数据
- **User Story 5 (P2)**: 依赖Foundational和US2 - US5需要US2的板件数据
- **User Story 6 (P3)**: 依赖Foundational和US2 - US6需要US2的板件数据
- **User Story 7 (P2)**: 依赖Foundational、US2和US6 - US7需要验证报工记录保留

### Within Each User Story

- DTO before Service
- Service before Controller
- Service逻辑实现完成后再添加日志和测试
- 每个故事完成后独立测试验收

### Parallel Opportunities

- **Setup (Phase 1)**: T004、T005、T006可并行
- **Foundational (Phase 2)**: 
  - T007-T016所有Entity可并行
  - T017-T026所有Mapper可并行
  - T027-T029 DTO和API客户端可并行
- **User Story 3/4/5 (P2)**: US3、US4、US5可并行开发（不同查询接口，不同文件）
- **Polish (Phase 10)**: T071、T072、T073、T075、T076可并行

---

## Parallel Example: Foundational Phase

```bash
# 并行创建所有Entity（不同文件，无依赖）:
Task T007: "Create Batch entity"
Task T008: "Create OptimizingFile entity"
Task T009: "Create WorkOrder entity"
Task T010: "Create PrePackageOrder entity"
Task T011: "Create Box entity"
Task T012: "Create Package entity"
Task T013: "Create Part entity"
Task T014: "Create WorkReport entity"
Task T015: "Create WorkOrderCorrectionLog entity"
Task T016: "Create EmailNotificationConfig entity"

# 并行创建所有Mapper（不同文件，无依赖）:
Task T017: "Create BatchMapper"
Task T018: "Create OptimizingFileMapper"
...
Task T026: "Create EmailNotificationConfigMapper"
```

---

## Parallel Example: P2 User Stories (US3/US4/US5)

```bash
# 三个查询功能可并行开发（不同接口，不同文件）:
Developer A: User Story 3 (板件码查询工单与批次)
Developer B: User Story 4 (板件码查询包装数据)
Developer C: User Story 5 (板件码查询板件详细信息)
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (批次推送)
4. Complete Phase 4: User Story 2 (预包装拉取)
5. **STOP and VALIDATE**: 测试批次推送→预包装拉取完整流程
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → 基础就绪
2. Add User Story 1 → 测试独立 → 可演示批次推送
3. Add User Story 2 → 测试独立 → 可演示完整数据拉取流程（MVP!）
4. Add User Story 3/4/5 → 测试独立 → 可演示产线查询功能
5. Add User Story 6 → 测试独立 → 可演示报工功能
6. Add User Story 7 → 测试独立 → 可演示数据修正功能
7. 每个故事独立交付价值，不破坏已有功能

### Parallel Team Strategy

多人开发团队：

1. 团队共同完成Setup + Foundational
2. Foundational完成后：
   - Developer A: User Story 1 + User Story 2 (核心P1功能)
   - Developer B: User Story 3 + User Story 4 (产线查询)
   - Developer C: User Story 5 + User Story 6 (板件详情+报工)
   - Developer D: User Story 7 (数据修正，需等待A和C完成)
3. 各故事独立完成并集成

---

## Task Summary

**Total Tasks**: 78

**Tasks per User Story**:
- Setup: 6 tasks
- Foundational: 23 tasks (BLOCKS ALL STORIES)
- User Story 1 (P1): 6 tasks
- User Story 2 (P1): 9 tasks
- User Story 3 (P2): 6 tasks
- User Story 4 (P2): 4 tasks
- User Story 5 (P2): 4 tasks
- User Story 6 (P3): 6 tasks
- User Story 7 (P2): 6 tasks
- Polish: 8 tasks

**Parallel Opportunities**: 
- Setup: 3 tasks (T004-T006)
- Foundational: 20 tasks (T007-T026)
- P2 Stories: US3/US4/US5 can run in parallel (3 developers)
- Polish: 5 tasks (T071-T073, T075-T076)

**Suggested MVP Scope**: 
- Phase 1 (Setup) + Phase 2 (Foundational) + Phase 3 (US1) + Phase 4 (US2)
- Total: 44 tasks
- Delivers: 批次推送 + 预包装数据自动拉取 + 邮件通知
- This is the core data integration pipeline

**Independent Test Criteria**:
- US1: 批次推送成功，数据入库，幂等性验证
- US2: 预包装自动拉取，重试机制，邮件通知，定时任务互斥
- US3: 板件码查询工单和批次信息，含优化文件
- US4: 板件码查询包装数据，四层嵌套结构
- US5: 板件码查询板件详细信息
- US6: 板件报工，幂等性验证
- US7: 工单数据修正，预包装覆盖，报工保留

**Format Validation**: ✅ All tasks follow checklist format (checkbox, ID, [P]/[Story] labels, file paths)

---

## Notes

- [P] tasks = 不同文件，无依赖，可并行
- [Story] label = 任务所属用户故事，便于追溯
- 每个用户故事独立可完成、可测试
- 避免：模糊任务、同文件冲突、破坏独立性的跨故事依赖
- 关键路径：Setup → Foundational → US1 → US2 → (US3/US4/US5并行) → US6 → US7 → Polish
- 预计工期：10-12个工作日（详见plan.md第5节）
