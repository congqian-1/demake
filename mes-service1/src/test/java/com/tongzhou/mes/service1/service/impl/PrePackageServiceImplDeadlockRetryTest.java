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
import com.tongzhou.mes.service1.mapper.MesBoardMapper;
import com.tongzhou.mes.service1.mapper.MesBoxCodeMapper;
import com.tongzhou.mes.service1.mapper.MesCorrectionLogMapper;
import com.tongzhou.mes.service1.mapper.MesPackageMapper;
import com.tongzhou.mes.service1.mapper.MesPrepackageOrderMapper;
import com.tongzhou.mes.service1.mapper.MesWorkOrderMapper;
import com.tongzhou.mes.service1.pojo.dto.PrepackageDataDTO;
import com.tongzhou.mes.service1.pojo.entity.MesWorkOrder;
import com.tongzhou.mes.service1.service.EmailNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 覆盖保存死锁重试测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrePackageServiceImpl 死锁重试测试")
class PrePackageServiceImplDeadlockRetryTest {

    @Mock private MesWorkOrderMapper workOrderMapper;
    @Mock private MesPrepackageOrderMapper prepackageOrderMapper;
    @Mock private MesBoxCodeMapper boxCodeMapper;
    @Mock private MesPackageMapper packageMapper;
    @Mock private MesBoardMapper boardMapper;
    @Mock private MesCorrectionLogMapper correctionLogMapper;
    @Mock private ThirdPartyMesClient thirdPartyMesClient;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private PrePackageOverwriteTxService prePackageOverwriteTxService;

    @InjectMocks
    private PrePackageServiceImpl service;

    @Test
    @DisplayName("覆盖保存遇到死锁时自动重试")
    void shouldRetryOverwriteSaveWhenDeadlockOccurs() {
        MesWorkOrder workOrder = new MesWorkOrder();
        workOrder.setWorkId("WD-DEADLOCK-001");
        PrepackageDataDTO data = new PrepackageDataDTO();
        AtomicInteger attempts = new AtomicInteger();

        doAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                throw new DeadlockLoserDataAccessException("Deadlock found when trying to get lock", null);
            }
            return null;
        }).when(prePackageOverwriteTxService).execute(any(Runnable.class));

        ReflectionTestUtils.invokeMethod(service, "savePrePackageDataWithOverwriteInNewTransaction", workOrder, data);

        assertEquals(2, attempts.get());
    }

    @Test
    @DisplayName("覆盖保存拿不到并发槽位时超时失败")
    void shouldFailFastWhenOverwriteSavePermitTimeout() throws InterruptedException {
        MesWorkOrder workOrder = new MesWorkOrder();
        workOrder.setWorkId("WD-TIMEOUT-001");
        PrepackageDataDTO data = new PrepackageDataDTO();
        Semaphore semaphore = (Semaphore) ReflectionTestUtils.getField(service, "overwriteSaveSemaphore");

        ReflectionTestUtils.setField(service, "overwriteSavePermitTimeoutSeconds", 0L);
        assertTrue(semaphore.tryAcquire(2));

        try {
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> ReflectionTestUtils.invokeMethod(service,
                            "savePrePackageDataWithOverwriteInNewTransaction", workOrder, data));
            assertTrue(exception.getMessage().contains("超时"));
            verifyNoInteractions(prePackageOverwriteTxService);
        } finally {
            semaphore.release(2);
        }
    }
}
