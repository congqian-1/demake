---

description: "Task list template for feature implementation"
---

# Tasks: 批次与包装层级查询

**Input**: Design documents from `/specs/002-batch-packaging-query/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are OPTIONAL - none requested in spec.md.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `mes-service1/src/main/java/...`
- Paths shown below follow plan.md structure for `mes-service1`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create base response DTOs `ResultBatchHierarchy.java`, `ResultPrepackageHierarchy.java`, `ErrorResponse.java` in `mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/hierarchy/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 Add not-found exceptions `BatchNotFoundException.java`, `PrepackageOrderNotFoundException.java` in `mes-service1/src/main/java/com/tongzhou/mes/service1/exception/`
- [X] T003 Add hierarchy exception handler `HierarchyExceptionHandler.java` in `mes-service1/src/main/java/com/tongzhou/mes/service1/exception/` to map 404/500 into `{ code, message, data }`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - 获取完整批次层级 (Priority: P1) 🎯 MVP

**Goal**: 按批次号返回批次及其全部下级层级（优化文件→工单→预包装订单→箱码→包件→板件），空层级返回 `[]`，不存在返回 404。

**Independent Test**: 使用已知批次号调用 `/api/v1/production/batches/{batchNum}/hierarchy`，确认返回完整层级结构与字段，缺失层级为空数组。

### Implementation for User Story 1

- [X] T004 [P] [US1] Create batch hierarchy DTOs `BatchHierarchy.java`, `BatchDTO.java`, `OptimizingFileDTO.java`, `WorkOrderDTO.java`, `PrepackageOrderDTO.java`, `BoxDTO.java`, `PackageDTO.java`, `PartDTO.java` in `mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/hierarchy/`
- [X] T005 [US1] Add service interface `BatchPackagingQueryService.java` with `getBatchHierarchy(String batchNum)` in `mes-service1/src/main/java/com/tongzhou/mes/service1/service/`
- [X] T006 [US1] Implement batch hierarchy aggregation with batch IN queries and empty-list defaults in `mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/BatchPackagingQueryServiceImpl.java`
- [X] T007 [US1] Modify `queryWorkOrderAndBatch` to return batch hierarchy in `mes-service1/src/main/java/com/tongzhou/mes/service1/controller/PartQueryController.java`
- [X] T008 [US1] Add/extend mapper query helpers for batch hierarchy lookups in `mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/MesBatchMapper.java`, `MesOptimizationFileMapper.java`, `MesWorkOrderMapper.java`, `MesPrepackageOrderMapper.java`, `MesBoxCodeMapper.java`, `MesPackageMapper.java`, `MesBoardMapper.java`

**Checkpoint**: User Story 1 should be fully functional and independently testable

---

## Phase 4: User Story 2 - 获取完整包装层级 (Priority: P2)

**Goal**: 按预包装订单号（可选工单号兜底）返回预包装订单及其下级层级（箱码→包件→板件），空层级返回 `[]`，不存在返回 404。

**Independent Test**: 使用已知订单号调用 `/api/v1/production/prepackage-orders/{orderNum}/hierarchy`，确认返回完整层级结构与字段，缺失层级为空数组。

### Implementation for User Story 2

- [X] T009 [P] [US2] Create prepackage hierarchy wrapper `PrepackageHierarchy.java` in `mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/hierarchy/`
- [X] T010 [US2] Extend `BatchPackagingQueryService.java` and `BatchPackagingQueryServiceImpl.java` with `getPrepackageHierarchy(String orderNum, String workId)` in `mes-service1/src/main/java/com/tongzhou/mes/service1/service/` and `mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/`
- [X] T011 [US2] Modify `queryPackage` to return prepackage hierarchy with optional `workId` in `mes-service1/src/main/java/com/tongzhou/mes/service1/controller/PartQueryController.java`

**Checkpoint**: User Story 2 should be fully functional and independently testable

---

## Phase 5: User Story 3 - 关联查看板件报工记录 (Priority: P3)

**Goal**: 在板件节点下返回匹配的报工记录（按 `partCode`），无记录返回空数组。

**Independent Test**: 查询含报工记录的批次或预包装订单，确认 `workReports` 出现在对应板件下。

### Implementation for User Story 3

- [X] T012 [P] [US3] Add `WorkReportDTO.java` and extend `PartDTO.java` with `workReports` in `mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/hierarchy/`
- [X] T013 [US3] Update hierarchy aggregation to batch-load `mes_work_report` by part codes and attach to parts in `mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/BatchPackagingQueryServiceImpl.java`
- [X] T014 [US3] Update controller annotations/response docs to include work report fields in `mes-service1/src/main/java/com/tongzhou/mes/service1/controller/PartQueryController.java`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T015 [P] Add SpringDoc annotations for both endpoints and schema references in `mes-service1/src/main/java/com/tongzhou/mes/service1/controller/PartQueryController.java`
- [X] T016 Run quickstart validation and update examples if needed in `specs/002-batch-packaging-query/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independent but can reuse US1 DTOs/services
- **User Story 3 (P3)**: Depends on US1 + US2 structure for part nodes to attach work reports

### Within Each User Story

- DTOs before services
- Services before endpoints
- Core implementation before integration

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- DTO tasks marked [P] can run in parallel within each story

---

## Parallel Example: User Story 1

```bash
# Launch DTO creation for User Story 1 together:
Task: "Create batch hierarchy DTOs BatchHierarchy.java, BatchDTO.java, OptimizingFileDTO.java, WorkOrderDTO.java, PrepackageOrderDTO.java, BoxDTO.java, PackageDTO.java, PartDTO.java"
```

---

## Parallel Example: User Story 2

```bash
# Launch DTO creation for User Story 2 together:
Task: "Create prepackage hierarchy wrapper PrepackageHierarchy.java"
```

---

## Parallel Example: User Story 3

```bash
# Launch DTO creation for User Story 3 together:
Task: "Add WorkReportDTO.java and extend PartDTO.java with workReports"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Avoid vague tasks, cross-story dependencies that break independence
