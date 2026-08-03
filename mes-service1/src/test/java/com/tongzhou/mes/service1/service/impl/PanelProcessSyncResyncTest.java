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
import com.tongzhou.mes.service1.service.PanelProcessSyncService.SyncResult;
import com.tongzhou.mes.service1.service.PrePackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PanelProcessSyncServiceImpl 批次重拉行为测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PanelProcessSyncServiceImpl 批次重拉测试")
class PanelProcessSyncResyncTest {

    @Mock private MesWorkOrderMapper workOrderMapper;
    @Mock private MesPanelProcessSyncMapper panelProcessSyncMapper;
    @Mock private ThirdPartyMesClient thirdPartyMesClient;
    @Mock private PrePackageService prePackageService;

    @InjectMocks
    private PanelProcessSyncServiceImpl service;

    private static final String BATCH_NUM = "PCJH-260701-0108";
    private static final String WORK_ID = "DDN000108801BCP001";
    private static final String PART_CODE = "DDN0001088011128";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "syncEnabled", true);
    }

    @Test
    @DisplayName("已经由接口同步过的批次直接跳过第三方重拉")
    void shouldSkipThirdPartyWhenBatchSyncedByQueryInterface() {
        when(panelProcessSyncMapper.countByBatchNum(BATCH_NUM)).thenReturn(1);

        SyncResult result = service.resyncBatchProcess(BATCH_NUM);

        assertTrue(result.isAlreadySynced());
        assertTrue(result.isSuccess());
        verify(panelProcessSyncMapper).countByBatchNum(BATCH_NUM);
        verifyNoInteractions(workOrderMapper, prePackageService, thirdPartyMesClient);
    }

    @Test
    @DisplayName("未由接口同步过的批次，由查询线程独占拉取")
    void shouldResyncByResettingThenUsingOriginalSyncFlow() {
        MesWorkOrder workOrder = buildWorkOrder();
        when(panelProcessSyncMapper.countByBatchNum(BATCH_NUM)).thenReturn(0);
        when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(Collections.singletonList(workOrder));
        when(prePackageService.repullSingleWorkOrderForSync(BATCH_NUM, WORK_ID))
                .thenReturn(buildPullResult("PULLED", null));

        SyncResult result = service.resyncBatchProcess(BATCH_NUM);

        assertTrue(result.isSuccess());
        verify(panelProcessSyncMapper).countByBatchNum(BATCH_NUM);
        verify(prePackageService).repullSingleWorkOrderForSync(BATCH_NUM, WORK_ID);
    }

    @Test
    @DisplayName("重拉失败时写入具体失败原因")
    void shouldWriteDetailedFailureReason() {
        MesWorkOrder workOrder = buildWorkOrder();
        when(panelProcessSyncMapper.countByBatchNum(BATCH_NUM)).thenReturn(0);
        when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(Collections.singletonList(workOrder));
        when(prePackageService.repullSingleWorkOrderForSync(BATCH_NUM, WORK_ID))
                .thenReturn(buildPullResult("FAILED", null));

        MesWorkOrder latest = buildWorkOrder();
        latest.setPrepackageStatus("FAILED");
        latest.setRetryCount(3);
        latest.setErrorMessage("第三方接口返回空数据");
        when(workOrderMapper.selectByBatchNumAndWorkId(BATCH_NUM, WORK_ID)).thenReturn(latest);

        SyncResult result = service.resyncBatchProcess(BATCH_NUM);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorDetail().contains("status=FAILED"));
        assertTrue(result.getErrorDetail().contains("第三方接口返回空数据"));
        verify(panelProcessSyncMapper).updateResult(eq(BATCH_NUM), eq(WORK_ID),
                eq("FAILED"), contains("第三方接口返回空数据"));
    }

    @Test
    @DisplayName("按板件发现批次时按接口同步去重触发原保存逻辑")
    void shouldDiscoverBatchAndResync() throws IOException {
        BatchQueryResponseDTO response = new BatchQueryResponseDTO();
        response.setCode(0);
        BatchQueryResponseDTO.BatchQueryItem item = new BatchQueryResponseDTO.BatchQueryItem();
        item.setFtm(PART_CODE);
        item.setFpjh(BATCH_NUM);
        response.setData(Collections.singletonList(item));
        when(thirdPartyMesClient.batchQueryProcess(anyList())).thenReturn(response);
        when(workOrderMapper.selectByBatchNum(BATCH_NUM)).thenReturn(Collections.emptyList());

        SyncResult result = service.discoverAndResyncByPartCode(PART_CODE);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(panelProcessSyncMapper).countByBatchNum(BATCH_NUM);
    }

    private MesWorkOrder buildWorkOrder() {
        MesWorkOrder workOrder = new MesWorkOrder();
        workOrder.setBatchNum(BATCH_NUM);
        workOrder.setWorkId(WORK_ID);
        workOrder.setPrepackageStatus("PULLED");
        workOrder.setIsDeleted(0);
        return workOrder;
    }

    private SyncPullResult buildPullResult(String status, String errorMessage) {
        SyncPullResult result = new SyncPullResult();
        result.setWorkId(WORK_ID);
        result.setStatus(status);
        result.setErrorMessage(errorMessage);
        result.setBoardCount("PULLED".equals(status) ? 10 : 0);
        return result;
    }
}
