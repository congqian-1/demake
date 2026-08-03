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

import com.tongzhou.mes.service1.client.ThirdPartyMesClient;
import com.tongzhou.mes.service1.mapper.MesPanelProcessSyncMapper;
import com.tongzhou.mes.service1.mapper.MesWorkOrderMapper;
import com.tongzhou.mes.service1.pojo.bo.SyncPullResult;
import com.tongzhou.mes.service1.pojo.dto.BatchQueryResponseDTO;
import com.tongzhou.mes.service1.pojo.entity.MesWorkOrder;
import com.tongzhou.mes.service1.service.PanelProcessSyncService;
import com.tongzhou.mes.service1.service.PanelProcessSyncService.SyncResult;
import com.tongzhou.mes.service1.service.PrePackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PanelProcessSyncServiceImpl 完整单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PanelProcessSyncServiceImpl 单元测试")
class PanelProcessSyncServiceImplTest {

    @Mock private MesWorkOrderMapper workOrderMapper;
    @Mock private MesPanelProcessSyncMapper panelProcessSyncMapper;
    @Mock private ThirdPartyMesClient thirdPartyMesClient;
    @Mock private PrePackageService prePackageService;

    @InjectMocks
    private PanelProcessSyncServiceImpl service;

    private static final String BATCH_NUM = "BATCH-001";
    private static final String WORK_ID_1 = "WO-001";
    private static final String WORK_ID_2 = "WO-002";
    private static final String PART_CODE = "PART-001";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "syncEnabled", true);
    }

    // ==================== syncBatchProcessIfNeeded ====================

    @Nested
    @DisplayName("syncBatchProcessIfNeeded")
    class SyncBatchProcessIfNeededTests {

        @Test
        @DisplayName("功能未开启时返回 failure")
        void shouldReturnFailureWhenDisabled() {
            ReflectionTestUtils.setField(service, "syncEnabled", false);

            SyncResult result = service.syncBatchProcessIfNeeded(BATCH_NUM);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("功能未开启"));
            verifyNoInteractions(panelProcessSyncMapper);
        }

        @Test
        @DisplayName("批次号为空时返回 failure")
        void shouldReturnFailureWhenBatchNumNull() {
            SyncResult result = service.syncBatchProcessIfNeeded(null);
            assertFalse(result.isSuccess());

            result = service.syncBatchProcessIfNeeded("");
            assertFalse(result.isSuccess());

            result = service.syncBatchProcessIfNeeded("  ");
            assertFalse(result.isSuccess());

            verifyNoInteractions(panelProcessSyncMapper);
        }

        @Test
        @DisplayName("批次已有同步记录时返回 alreadySynced")
        void shouldReturnAlreadySyncedWhenRecordsExist() {
            when(panelProcessSyncMapper.countByBatchNum(BATCH_NUM)).thenReturn(1);

            SyncResult result = service.syncBatchProcessIfNeeded(BATCH_NUM);

            assertTrue(result.isAlreadySynced());
            assertTrue(result.isSuccess());
            verify(panelProcessSyncMapper, never()).insert(any(com.tongzhou.mes.service1.pojo.entity.MesPanelProcessSync.class));
        }

        @Test
        @DisplayName("批次下没有工单时返回 success(0)")
        void shouldReturnSuccessWhenNoWorkOrders() {
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(Collections.emptyList());

            SyncResult result = service.syncBatchProcessIfNeeded(BATCH_NUM);

            assertTrue(result.isSuccess());
            assertEquals(0, result.getUpdatedBoardCount());
            // 无工单时插入占位记录用于去重
            verify(panelProcessSyncMapper).insert(any(com.tongzhou.mes.service1.pojo.entity.MesPanelProcessSync.class));
        }

        @Test
        @DisplayName("全部工单同步成功")
        void shouldReturnSuccessWhenAllWorkOrdersSucceed() {
            List<MesWorkOrder> workOrders = createWorkOrders(WORK_ID_1, WORK_ID_2);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(workOrders);

            SyncPullResult pullResult1 = buildPullResult(WORK_ID_1, "PULLED", 10);
            SyncPullResult pullResult2 = buildPullResult(WORK_ID_2, "PULLED", 5);
            when(prePackageService.pullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_1))
                    .thenReturn(pullResult1);
            when(prePackageService.pullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_2))
                    .thenReturn(pullResult2);

            SyncResult result = service.syncBatchProcessIfNeeded(BATCH_NUM);

            assertTrue(result.isSuccess());
            assertEquals(15, result.getUpdatedBoardCount());
            assertNull(result.getErrorDetail());
            verify(panelProcessSyncMapper, times(2)).insert(any(com.tongzhou.mes.service1.pojo.entity.MesPanelProcessSync.class));
            verify(panelProcessSyncMapper).updateResult(eq(BATCH_NUM), eq(WORK_ID_1),
                    eq("SUCCESS"), isNull());
            verify(panelProcessSyncMapper).updateResult(eq(BATCH_NUM), eq(WORK_ID_2),
                    eq("SUCCESS"), isNull());
        }

        @Test
        @DisplayName("部分工单失败返回 partialFailure")
        void shouldReturnPartialFailureWhenSomeFail() {
            List<MesWorkOrder> workOrders = createWorkOrders(WORK_ID_1, WORK_ID_2);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(workOrders);

            SyncPullResult pullResult1 = buildPullResult(WORK_ID_1, "PULLED", 10);
            SyncPullResult pullResult2 = buildPullResult(WORK_ID_2, "FAILED", 0, "ERR-001", "MES超时");
            when(prePackageService.pullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_1))
                    .thenReturn(pullResult1);
            when(prePackageService.pullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_2))
                    .thenReturn(pullResult2);

            SyncResult result = service.syncBatchProcessIfNeeded(BATCH_NUM);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("部分失败"));
            assertTrue(result.getErrorDetail().contains(WORK_ID_2));
            assertTrue(result.getErrorDetail().contains("MES超时"));
            assertEquals(10, result.getUpdatedBoardCount());
        }

        @Test
        @DisplayName("全部工单失败返回 failure")
        void shouldReturnFailureWhenAllFail() {
            List<MesWorkOrder> workOrders = createWorkOrders(WORK_ID_1, WORK_ID_2);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(workOrders);

            SyncPullResult pullResult1 = buildPullResult(WORK_ID_1, "FAILED", 0, "ERR-001", "网络错误");
            SyncPullResult pullResult2 = buildPullResult(WORK_ID_2, "FAILED", 0, "ERR-002", "解析失败");
            when(prePackageService.pullSingleWorkOrderForSync(anyString(), anyString()))
                    .thenReturn(pullResult1, pullResult2);

            SyncResult result = service.syncBatchProcessIfNeeded(BATCH_NUM);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("全部失败"));
            assertTrue(result.getErrorDetail().contains("网络错误"));
            assertTrue(result.getErrorDetail().contains("解析失败"));
        }

        @Test
        @DisplayName("工单正在处理中(PROCESSING)视为成功")
        void shouldTreatProcessingAsSuccess() {
            List<MesWorkOrder> workOrders = createWorkOrders(WORK_ID_1);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(workOrders);

            SyncPullResult pullResult = buildPullResult(WORK_ID_1, "PROCESSING", 0);
            when(prePackageService.pullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_1))
                    .thenReturn(pullResult);

            SyncResult result = service.syncBatchProcessIfNeeded(BATCH_NUM);

            assertTrue(result.isSuccess());
            verify(panelProcessSyncMapper).updateResult(eq(BATCH_NUM), eq(WORK_ID_1),
                    eq("SUCCESS"), contains("正在处理中"));
        }

        @Test
        @DisplayName("pullSingleWorkOrderForSync 抛异常时记录失败")
        void shouldRecordFailureWhenExceptionThrown() {
            List<MesWorkOrder> workOrders = createWorkOrders(WORK_ID_1);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(workOrders);
            when(prePackageService.pullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_1))
                    .thenThrow(new RuntimeException("意外异常"));

            SyncResult result = service.syncBatchProcessIfNeeded(BATCH_NUM);

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorDetail().contains("意外异常"));
            verify(panelProcessSyncMapper).updateResult(eq(BATCH_NUM), eq(WORK_ID_1),
                    eq("FAILED"), contains("意外异常"));
        }

        @Test
        @DisplayName("并发插入时唯一键冲突仍正常处理")
        void shouldHandleDuplicateInsertGracefully() {
            List<MesWorkOrder> workOrders = createWorkOrders(WORK_ID_1);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(workOrders);
            when(panelProcessSyncMapper.insert(any(com.tongzhou.mes.service1.pojo.entity.MesPanelProcessSync.class)))
                    .thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));
            when(prePackageService.pullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_1))
                    .thenReturn(buildPullResult(WORK_ID_1, "PULLED", 3));

            SyncResult result = service.syncBatchProcessIfNeeded(BATCH_NUM);

            assertTrue(result.isSuccess());
            assertEquals(3, result.getUpdatedBoardCount());
        }

        @Test
        @DisplayName("未由接口同步过的批次，由查询线程独占拉取")
        void shouldResyncByResettingThenUsingOriginalSyncFlow() {
            List<MesWorkOrder> workOrders = createWorkOrders(WORK_ID_1);
            when(panelProcessSyncMapper.countByBatchNum(BATCH_NUM)).thenReturn(0);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(workOrders);
            when(prePackageService.repullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_1))
                    .thenReturn(buildPullResult(WORK_ID_1, "PULLED", 7));

            SyncResult result = service.resyncBatchProcess(BATCH_NUM);

            assertTrue(result.isSuccess());
            assertEquals(7, result.getUpdatedBoardCount());
            verify(panelProcessSyncMapper).countByBatchNum(BATCH_NUM);
            verify(prePackageService).repullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_1);
        }

        @Test
        @DisplayName("失败结果缺少错误信息时从工单表补充原因")
        void shouldFillFailureDetailFromWorkOrderWhenMissingMessage() {
            List<MesWorkOrder> workOrders = createWorkOrders(WORK_ID_1);
            when(panelProcessSyncMapper.countByBatchNum(BATCH_NUM)).thenReturn(0);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(workOrders);
            when(prePackageService.repullSingleWorkOrderForSync(BATCH_NUM, WORK_ID_1))
                    .thenReturn(buildPullResult(WORK_ID_1, "FAILED", 0));

            MesWorkOrder latest = new MesWorkOrder();
            latest.setBatchNum(BATCH_NUM);
            latest.setWorkId(WORK_ID_1);
            latest.setPrepackageStatus("FAILED");
            latest.setRetryCount(3);
            latest.setErrorMessage("第三方接口返回空数据");
            when(workOrderMapper.selectByBatchNumAndWorkId(BATCH_NUM, WORK_ID_1)).thenReturn(latest);

            SyncResult result = service.resyncBatchProcess(BATCH_NUM);

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorDetail().contains("status=FAILED"));
            assertTrue(result.getErrorDetail().contains("第三方接口返回空数据"));
            verify(panelProcessSyncMapper).updateResult(eq(BATCH_NUM), eq(WORK_ID_1),
                    eq("FAILED"), contains("第三方接口返回空数据"));
        }
    }

    // ==================== discoverAndSyncByPartCode ====================

    @Nested
    @DisplayName("discoverAndSyncByPartCode")
    class DiscoverAndSyncByPartCodeTests {

        @Test
        @DisplayName("功能未开启时返回 null")
        void shouldReturnNullWhenDisabled() {
            ReflectionTestUtils.setField(service, "syncEnabled", false);
            assertNull(service.discoverAndSyncByPartCode(PART_CODE));
            verifyNoInteractions(thirdPartyMesClient);
        }

        @Test
        @DisplayName("板件码为空时返回 null")
        void shouldReturnNullWhenPartCodeEmpty() {
            assertNull(service.discoverAndSyncByPartCode(null));
            assertNull(service.discoverAndSyncByPartCode(""));
            verifyNoInteractions(thirdPartyMesClient);
        }

        @Test
        @DisplayName("MES 返回有效批次时触发同步并返回 SyncResult")
        void shouldReturnSyncResultWhenMesReturnsBatch() throws IOException {
            BatchQueryResponseDTO response = buildBatchQueryResponse(PART_CODE, BATCH_NUM);
            when(thirdPartyMesClient.batchQueryProcess(anyList())).thenReturn(response);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(Collections.emptyList());

            SyncResult result = service.discoverAndSyncByPartCode(PART_CODE);

            assertNotNull(result);
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("重拉发现批次时按接口同步去重触发原保存逻辑")
        void shouldResyncWhenMesReturnsBatch() throws IOException {
            BatchQueryResponseDTO response = buildBatchQueryResponse(PART_CODE, BATCH_NUM);
            when(thirdPartyMesClient.batchQueryProcess(anyList())).thenReturn(response);
            when(panelProcessSyncMapper.countByBatchNum(BATCH_NUM)).thenReturn(0);
            when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(Collections.emptyList());

            SyncResult result = service.discoverAndResyncByPartCode(PART_CODE);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            verify(panelProcessSyncMapper).countByBatchNum(BATCH_NUM);
        }

        @Test
        @DisplayName("MES 返回 null 时返回 null")
        void shouldReturnNullWhenMesReturnsNull() throws IOException {
            when(thirdPartyMesClient.batchQueryProcess(anyList())).thenReturn(null);

            assertNull(service.discoverAndSyncByPartCode(PART_CODE));
        }

        @Test
        @DisplayName("MES 返回 code != 0 时返回 null")
        void shouldReturnNullWhenMesCodeNotZero() throws IOException {
            BatchQueryResponseDTO response = new BatchQueryResponseDTO();
            response.setCode(1);
            response.setMsg("error");
            when(thirdPartyMesClient.batchQueryProcess(anyList())).thenReturn(response);

            assertNull(service.discoverAndSyncByPartCode(PART_CODE));
        }

        @Test
        @DisplayName("MES 返回空 data 时返回 null")
        void shouldReturnNullWhenMesDataEmpty() throws IOException {
            BatchQueryResponseDTO response = new BatchQueryResponseDTO();
            response.setCode(0);
            response.setData(Collections.emptyList());
            when(thirdPartyMesClient.batchQueryProcess(anyList())).thenReturn(response);

            assertNull(service.discoverAndSyncByPartCode(PART_CODE));
        }

        @Test
        @DisplayName("MES 返回的 FTM 不匹配时返回 null")
        void shouldReturnNullWhenFtmNotMatch() throws IOException {
            BatchQueryResponseDTO response = buildBatchQueryResponse("OTHER-PART", BATCH_NUM);
            when(thirdPartyMesClient.batchQueryProcess(anyList())).thenReturn(response);

            assertNull(service.discoverAndSyncByPartCode(PART_CODE));
        }

        @Test
        @DisplayName("MES 返回的 FPJH 为空时返回 null")
        void shouldReturnNullWhenFpjhEmpty() throws IOException {
            BatchQueryResponseDTO response = buildBatchQueryResponse(PART_CODE, "");
            when(thirdPartyMesClient.batchQueryProcess(anyList())).thenReturn(response);

            assertNull(service.discoverAndSyncByPartCode(PART_CODE));
        }

        @Test
        @DisplayName("MES 调用抛异常时返回 null")
        void shouldReturnNullWhenMesThrows() throws IOException {
            when(thirdPartyMesClient.batchQueryProcess(anyList()))
                    .thenThrow(new IOException("网络不通"));

            assertNull(service.discoverAndSyncByPartCode(PART_CODE));
        }
    }

    // ==================== SyncResult ====================

    @Nested
    @DisplayName("SyncResult 工厂方法")
    class SyncResultFactoryTests {

        @Test
        @DisplayName("alreadySynced 返回正确的属性")
        void alreadySynced() {
            SyncResult r = SyncResult.alreadySynced();
            assertTrue(r.isAlreadySynced());
            assertTrue(r.isSuccess());
            assertEquals(0, r.getUpdatedBoardCount());
            assertNull(r.getErrorDetail());
        }

        @Test
        @DisplayName("success 返回正确的属性")
        void success() {
            SyncResult r = SyncResult.success("完成", 42);
            assertFalse(r.isAlreadySynced());
            assertTrue(r.isSuccess());
            assertEquals(42, r.getUpdatedBoardCount());
            assertEquals("完成", r.getMessage());
            assertNull(r.getErrorDetail());
        }

        @Test
        @DisplayName("failure 返回正确的属性")
        void failure() {
            SyncResult r = SyncResult.failure("失败", "详细原因");
            assertFalse(r.isAlreadySynced());
            assertFalse(r.isSuccess());
            assertEquals(0, r.getUpdatedBoardCount());
            assertEquals("失败", r.getMessage());
            assertEquals("详细原因", r.getErrorDetail());
        }

        @Test
        @DisplayName("partialFailure 返回正确的属性")
        void partialFailure() {
            SyncResult r = SyncResult.partialFailure("部分失败", "3个工单异常", 10);
            assertFalse(r.isAlreadySynced());
            assertFalse(r.isSuccess());
            assertEquals(10, r.getUpdatedBoardCount());
            assertEquals("3个工单异常", r.getErrorDetail());
        }
    }

    // ==================== helpers ====================

    private List<MesWorkOrder> createWorkOrders(String... workIds) {
        List<MesWorkOrder> list = new ArrayList<>();
        for (String workId : workIds) {
            MesWorkOrder wo = new MesWorkOrder();
            wo.setWorkId(workId);
            wo.setBatchNum(BATCH_NUM);
            list.add(wo);
        }
        return list;
    }

    private SyncPullResult buildPullResult(String workId, String status, long boardCount) {
        return buildPullResult(workId, status, boardCount, null, null);
    }

    private SyncPullResult buildPullResult(String workId, String status, long boardCount,
                                            String errorCode, String errorMessage) {
        SyncPullResult r = new SyncPullResult();
        r.setWorkId(workId);
        r.setStatus(status);
        r.setBoardCount(boardCount);
        r.setErrorCode(errorCode);
        r.setErrorMessage(errorMessage);
        return r;
    }

    private BatchQueryResponseDTO buildBatchQueryResponse(String ftm, String fpjh) {
        BatchQueryResponseDTO response = new BatchQueryResponseDTO();
        response.setCode(0);
        response.setMsg("执行成功");
        BatchQueryResponseDTO.BatchQueryItem item = new BatchQueryResponseDTO.BatchQueryItem();
        item.setFtm(ftm);
        item.setFpjh(fpjh);
        item.setFgnym("DW020:包装");
        response.setData(Collections.singletonList(item));
        return response;
    }
}
