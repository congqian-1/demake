/*
 * Copyright (c) 2022 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tongzhou.mes.service1.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongzhou.mes.service1.mapper.MesBatchMapper;
import com.tongzhou.mes.service1.mapper.MesBoardMapper;
import com.tongzhou.mes.service1.mapper.MesBoxCodeMapper;
import com.tongzhou.mes.service1.mapper.MesOptimizationFileMapper;
import com.tongzhou.mes.service1.mapper.MesPackageMapper;
import com.tongzhou.mes.service1.mapper.MesPrepackageOrderMapper;
import com.tongzhou.mes.service1.mapper.MesWorkOrderMapper;
import com.tongzhou.mes.service1.mapper.MesWorkReportMapper;
import com.tongzhou.mes.service1.pojo.dto.hierarchy.BatchHierarchy;
import com.tongzhou.mes.service1.pojo.dto.hierarchy.ResultBatchHierarchy;
import com.tongzhou.mes.service1.pojo.entity.MesBoard;
import com.tongzhou.mes.service1.pojo.entity.MesWorkOrder;
import com.tongzhou.mes.service1.service.BatchPackagingQueryService;
import com.tongzhou.mes.service1.service.PanelProcessSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PartQueryServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PartQueryServiceImpl 单元测试")
class PartQueryServiceImplTest {

    @Mock private MesBoardMapper boardMapper;
    @Mock private MesWorkOrderMapper workOrderMapper;
    @Mock private MesBatchMapper batchMapper;
    @Mock private MesOptimizationFileMapper optimizationFileMapper;
    @Mock private MesPrepackageOrderMapper prepackageOrderMapper;
    @Mock private MesBoxCodeMapper boxCodeMapper;
    @Mock private MesPackageMapper packageMapper;
    @Mock private MesWorkReportMapper workReportMapper;
    @Mock private BatchPackagingQueryService batchPackagingQueryService;
    @Mock private ObjectMapper objectMapper;
    @Mock private PanelProcessSyncService panelProcessSyncService;

    @InjectMocks
    private PartQueryServiceImpl service;

    private static final String PART_CODE = "DDN0001088011128";
    private static final String BATCH_NUM = "PCJH-260701-0108";
    private static final String WORK_ID = "DDN000108801BCP001";

    @Test
    @DisplayName("queryWorkOrderAndBatch: 本地已有板件也按原逻辑重拉批次")
    void shouldResyncBatchWhenPartExistsLocally() {
        MesBoard board = buildBoard();
        MesWorkOrder workOrder = buildWorkOrder("PULLED");
        BatchHierarchy hierarchy = new BatchHierarchy();
        PanelProcessSyncService.SyncResult syncResult =
                PanelProcessSyncService.SyncResult.success("同步完成", 12);

        when(boardMapper.selectOne(anyBoardWrapper())).thenReturn(board, board);
        when(workOrderMapper.selectByBatchNumAndWorkId(BATCH_NUM, WORK_ID)).thenReturn(workOrder, workOrder);
        when(panelProcessSyncService.resyncBatchProcess(BATCH_NUM)).thenReturn(syncResult);
        when(batchPackagingQueryService.getBatchHierarchy(BATCH_NUM)).thenReturn(hierarchy);

        ResultBatchHierarchy result = service.queryWorkOrderAndBatch(PART_CODE);

        assertEquals("0", result.getCode());
        assertNotNull(result.getSync());
        assertEquals(12, result.getSync().getUpdatedBoardCount());
        verify(panelProcessSyncService).resyncBatchProcess(BATCH_NUM);
        verify(panelProcessSyncService, never()).syncBatchProcessIfNeeded(anyString());
        verify(panelProcessSyncService, never()).discoverAndSyncByPartCode(anyString());
    }

    @Test
    @DisplayName("queryWorkOrderAndBatch: 本地没有板件时发现批次并按原逻辑重拉")
    void shouldDiscoverAndResyncWhenPartMissingLocally() {
        MesBoard board = buildBoard();
        MesWorkOrder workOrder = buildWorkOrder("PULLED");
        BatchHierarchy hierarchy = new BatchHierarchy();
        PanelProcessSyncService.SyncResult syncResult =
                PanelProcessSyncService.SyncResult.success("同步完成", 12);

        when(boardMapper.selectOne(anyBoardWrapper())).thenReturn(null, board);
        when(panelProcessSyncService.discoverAndResyncByPartCode(PART_CODE)).thenReturn(syncResult);
        when(workOrderMapper.selectByBatchNumAndWorkId(BATCH_NUM, WORK_ID)).thenReturn(workOrder);
        when(batchPackagingQueryService.getBatchHierarchy(BATCH_NUM)).thenReturn(hierarchy);

        ResultBatchHierarchy result = service.queryWorkOrderAndBatch(PART_CODE);

        assertEquals("0", result.getCode());
        assertNotNull(result.getSync());
        verify(panelProcessSyncService).discoverAndResyncByPartCode(PART_CODE);
        verify(panelProcessSyncService, never()).discoverAndSyncByPartCode(anyString());
        verify(panelProcessSyncService, never()).syncBatchProcessIfNeeded(anyString());
    }

    @Test
    @DisplayName("queryWorkOrderAndBatch: 同步失败详情返回给前端时截断")
    void shouldTruncateSyncErrorDetailForClient() {
        MesBoard board = buildBoard();
        MesWorkOrder workOrder = buildWorkOrder("PULLED");
        BatchHierarchy hierarchy = new BatchHierarchy();
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            detail.append("工单 WO-").append(i).append(": 第三方接口返回空数据; ");
        }
        PanelProcessSyncService.SyncResult syncResult =
                PanelProcessSyncService.SyncResult.partialFailure("部分失败：成功 1/20 个工单",
                        detail.toString(), 3);

        when(boardMapper.selectOne(anyBoardWrapper())).thenReturn(board, board);
        when(workOrderMapper.selectByBatchNumAndWorkId(BATCH_NUM, WORK_ID)).thenReturn(workOrder, workOrder);
        when(panelProcessSyncService.resyncBatchProcess(BATCH_NUM)).thenReturn(syncResult);
        when(batchPackagingQueryService.getBatchHierarchy(BATCH_NUM)).thenReturn(hierarchy);

        ResultBatchHierarchy result = service.queryWorkOrderAndBatch(PART_CODE);

        assertNotNull(result.getSync());
        assertTrue(result.getSync().getErrorDetail().length() < detail.length());
        assertTrue(result.getSync().getErrorDetail().contains("已截断"));
    }

    private MesBoard buildBoard() {
        MesBoard board = new MesBoard();
        board.setPartCode(PART_CODE);
        board.setBatchNum(BATCH_NUM);
        board.setWorkId(WORK_ID);
        board.setIsDeleted(0);
        return board;
    }

    private MesWorkOrder buildWorkOrder(String status) {
        MesWorkOrder workOrder = new MesWorkOrder();
        workOrder.setBatchNum(BATCH_NUM);
        workOrder.setWorkId(WORK_ID);
        workOrder.setPrepackageStatus(status);
        workOrder.setIsDeleted(0);
        return workOrder;
    }

    private Wrapper<MesBoard> anyBoardWrapper() {
        return any();
    }
}
