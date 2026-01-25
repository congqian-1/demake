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

package com.tongzhou.mes.service1.scheduled;

import com.tongzhou.mes.service1.service.PrePackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 预包装数据拉取定时任务
 * 每1秒执行一次，使用互斥锁避免并发执行
 *
 * @author MES Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrePackagePullTask {

    private final PrePackageService prePackageService;

    /**
     * 互斥锁，确保同一时间只有一个任务在执行
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 定时拉取预包装数据
     * fixedDelay = 1000ms：上一次执行完成后，延迟1秒再执行下一次
     */
    @Scheduled(fixedDelay = 1000)
    public void pullPrePackageData() {
        // 尝试获取锁
        if (!isRunning.compareAndSet(false, true)) {
            log.info("预包装数据拉取任务正在执行中，跳过本次调度");
            return;
        }

        try {
            log.debug("开始执行预包装数据拉取定时任务");
            long startTime = System.currentTimeMillis();

            // 执行拉取任务
            prePackageService.pullPendingWorkOrders();

            long endTime = System.currentTimeMillis();
            log.debug("预包装数据拉取定时任务执行完成，耗时: {}ms", endTime - startTime);

        } catch (Exception e) {
            log.error("预包装数据拉取定时任务执行失败: {}", e.getMessage(), e);
        } finally {
            // 释放锁
            isRunning.set(false);
        }
    }
}
