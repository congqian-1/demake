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

import com.tongzhou.mes.service1.converter.BatchConverter;
import com.tongzhou.mes.service1.mapper.MesBatchMapper;
import com.tongzhou.mes.service1.mapper.MesBoardMapper;
import com.tongzhou.mes.service1.mapper.MesOptimizationFileMapper;
import com.tongzhou.mes.service1.mapper.MesWorkOrderMapper;
import com.tongzhou.mes.service1.pojo.bo.BatchSaveResult;
import com.tongzhou.mes.service1.pojo.dto.BatchPushRequest;
import com.tongzhou.mes.service1.pojo.entity.MesWorkOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次推送死锁保护测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BatchServiceImpl 死锁保护测试")
class BatchServiceImplDeadlockRetryTest {

    @Mock private MesBatchMapper batchMapper;
    @Mock private MesBoardMapper boardMapper;
    @Mock private MesOptimizationFileMapper optimizationFileMapper;
    @Mock private MesWorkOrderMapper workOrderMapper;
    @Mock private BatchConverter batchConverter;
    @Mock private BatchSaveTxService batchSaveTxService;

    @InjectMocks
    private BatchServiceImpl service;

    @Test
    @DisplayName("板件清理跳过正在拉取的工单")
    void shouldSkipBoardCleanupForUpdatingWorkOrder() {
        String batchNum = "BATCH-001";
        String updatingWorkId = "WO-UPDATING";
        String normalWorkId = "WO-NORMAL";

        when(workOrderMapper.selectByBatchNumAndWorkId(batchNum, updatingWorkId))
            .thenReturn(workOrder(updatingWorkId, "UPDATING"));
        when(workOrderMapper.selectByBatchNumAndWorkId(batchNum, normalWorkId))
            .thenReturn(workOrder(normalWorkId, "NOT_PULLED"));
        when(boardMapper.physicalDeleteByBatchNumAndWorkId(batchNum, normalWorkId)).thenReturn(3);

        int deleted = ReflectionTestUtils.invokeMethod(
            service,
            "deleteBoardsForTargetedWorkIds",
            batchNum,
            Arrays.asList(updatingWorkId, normalWorkId)
        );

        assertEquals(3, deleted);
        verify(boardMapper).physicalDeleteByBatchNumAndWorkId(batchNum, normalWorkId);
        verify(boardMapper, never()).physicalDeleteByBatchNumAndWorkId(eq(batchNum), eq(updatingWorkId));
    }

    @Test
    @DisplayName("批次保存遇到死锁时自动重试")
    void shouldRetryBatchSaveWhenDeadlockOccurs() {
        BatchPushRequest request = new BatchPushRequest();
        request.setBatchNum("BATCH-001");
        BatchSaveResult expected = new BatchSaveResult();
        expected.setBatchNum("BATCH-001");
        AtomicInteger attempts = new AtomicInteger();

        doAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                throw new DeadlockLoserDataAccessException("Deadlock found when trying to get lock", null);
            }
            return expected;
        }).when(batchSaveTxService).execute(any(Supplier.class));

        BatchSaveResult actual = service.saveBatchWithResult(request);

        assertSame(expected, actual);
        assertEquals(2, attempts.get());
        verify(batchSaveTxService, times(2)).execute(any(Supplier.class));
    }

    @Test
    @DisplayName("死锁重试达到上限后抛出异常")
    void shouldFailAfterMaxDeadlockRetries() {
        BatchPushRequest request = new BatchPushRequest();
        request.setBatchNum("BATCH-001");

        when(batchSaveTxService.execute(any(Supplier.class)))
            .thenThrow(new DeadlockLoserDataAccessException("Deadlock found when trying to get lock", null));

        assertThrows(DeadlockLoserDataAccessException.class, () -> service.saveBatchWithResult(request));
        verify(batchSaveTxService, times(3)).execute(any(Supplier.class));
    }

    private MesWorkOrder workOrder(String workId, String status) {
        MesWorkOrder workOrder = new MesWorkOrder();
        workOrder.setWorkId(workId);
        workOrder.setPrepackageStatus(status);
        return workOrder;
    }
}
