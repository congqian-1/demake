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
import com.tongzhou.mes.service1.pojo.entity.MesPanelProcessSync;
import com.tongzhou.mes.service1.pojo.entity.MesWorkOrder;
import com.tongzhou.mes.service1.service.PanelProcessSyncService;
import com.tongzhou.mes.service1.service.PrePackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 看板工序同步服务实现。
 * 数据库去重（按批次），每个工单调用现有 pullSingleWorkOrderForSync 同步数据并独立记录结果。
 *
 * @author MES Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PanelProcessSyncServiceImpl implements PanelProcessSyncService {

    private final MesWorkOrderMapper workOrderMapper;
    private final ThirdPartyMesClient thirdPartyMesClient;
    private final MesPanelProcessSyncMapper panelProcessSyncMapper;
    private final PrePackageService prePackageService;

    @Value("${mes.panel.process.sync.enabled:true}")
    private boolean syncEnabled;

    /**
     * 同步专用线程池，并行调用 pullSingleWorkOrderForSync 以提升大批次性能。
     * 调用方通过 CompletableFuture.allOf().join() 阻塞等待全部完成，
     * 对外表现为同步执行。
     */
    private final ExecutorService syncExecutor = new ThreadPoolExecutor(
            4, 8, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            r -> {
                Thread t = new Thread(r, "panel-process-sync-");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private enum PullMode {
        NORMAL,
        RESYNC
    }

    @Override
    public SyncResult syncBatchProcessIfNeeded(String batchNum) {
        if (!syncEnabled) {
            return SyncResult.failure("功能未开启", "mes.panel.process.sync.enabled=false");
        }
        if (batchNum == null || batchNum.trim().isEmpty()) {
            return SyncResult.failure("批次号为空", null);
        }

        // 数据库去重：该批次只要有一条记录即视为已同步
        if (panelProcessSyncMapper.countByBatchNum(batchNum) > 0) {
            log.debug("批次 {} 已有同步记录，跳过", batchNum);
            return SyncResult.alreadySynced();
        }

        return syncBatchProcess(batchNum, PullMode.NORMAL);
    }

    @Override
    public SyncResult resyncBatchProcess(String batchNum) {
        if (!syncEnabled) {
            return SyncResult.failure("功能未开启", "mes.panel.process.sync.enabled=false");
        }
        if (batchNum == null || batchNum.trim().isEmpty()) {
            return SyncResult.failure("批次号为空", null);
        }

        // 接口级去重：同一批次被该接口实际同步过一次后，后续查询只读库内数据。
        if (panelProcessSyncMapper.countByBatchNum(batchNum) > 0) {
            log.info("批次 {} 已由查询接口触发过同步，本次跳过第三方重拉", batchNum);
            return SyncResult.alreadySynced();
        }

        try {
            log.info("批次 {} 首次由查询接口触发同步，按原保存逻辑重新拉取", batchNum);
            return syncBatchProcess(batchNum, PullMode.RESYNC);
        } catch (Exception e) {
            log.error("批次 {} 查询接口触发同步异常: {}", batchNum, e.getMessage(), e);
            return SyncResult.failure("查询接口触发同步异常: " + e.getMessage(), e.getMessage());
        }
    }

    private SyncResult syncBatchProcess(String batchNum, PullMode pullMode) {
        log.info("开始同步批次 {} 下所有工单数据", batchNum);
        long startTime = System.currentTimeMillis();

        try {
            List<MesWorkOrder> workOrders = workOrderMapper.selectByBatchNum(batchNum);
            if (workOrders == null || workOrders.isEmpty()) {
                log.info("批次 {} 下没有工单，跳过同步", batchNum);
                insertRecord(batchNum, "__EMPTY__");
                updateRecord(batchNum, "__EMPTY__", "SUCCESS", null);
                return SyncResult.success("批次下没有工单", 0);
            }

            log.info("批次 {} 共有 {} 个工单需要同步", batchNum, workOrders.size());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            AtomicInteger totalBoardCount = new AtomicInteger(0);
            List<String> errors = new ArrayList<>();

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (MesWorkOrder workOrder : workOrders) {
                String workId = workOrder.getWorkId();
                insertRecord(batchNum, workId);

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        SyncPullResult pullResult = pullMode == PullMode.RESYNC
                                ? prePackageService.repullSingleWorkOrderForSync(batchNum, workId)
                                : prePackageService.pullSingleWorkOrderForSync(batchNum, workId);
                        String status = pullResult != null ? pullResult.getStatus() : null;
                        if ("PULLED".equals(status)) {
                            totalBoardCount.addAndGet((int) pullResult.getBoardCount());
                            updateRecord(batchNum, workId, "SUCCESS", null);
                            successCount.incrementAndGet();
                        } else if ("PROCESSING".equals(status)) {
                            updateRecord(batchNum, workId, "SUCCESS", "工单正在处理中，本次跳过");
                            successCount.incrementAndGet();
                        } else {
                            String errMsg = buildFailureDetail(batchNum, workId, pullResult);
                            log.warn("批次 {} 工单 {} 同步失败: {}", batchNum, workId, errMsg);
                            updateRecord(batchNum, workId, "FAILED", errMsg);
                            synchronized (errors) {
                                errors.add("工单 " + workId + ": " + errMsg);
                            }
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        String errMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
                        log.error("同步工单 {} 失败: {}", workId, errMsg, e);
                        updateRecord(batchNum, workId, "FAILED", errMsg);
                        synchronized (errors) {
                            errors.add("工单 " + workId + ": " + errMsg);
                        }
                        failCount.incrementAndGet();
                    }
                }, syncExecutor);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            long elapsed = System.currentTimeMillis() - startTime;
            int total = workOrders.size();
            int success = successCount.get();
            int failed = failCount.get();

            if (failed == 0) {
                log.info("批次 {} 同步全部成功，工单: {}, 板件: {}, 耗时: {}ms",
                        batchNum, total, totalBoardCount.get(), elapsed);
                return SyncResult.success(
                        "同步完成，" + total + " 个工单全部成功，共 " + totalBoardCount.get() + " 个板件",
                        totalBoardCount.get());
            } else if (success > 0) {
                String errorDetail = String.join("; ", errors);
                log.warn("批次 {} 同步部分失败，成功: {}, 失败: {}, 耗时: {}ms，失败明细: {}",
                        batchNum, success, failed, elapsed, errorDetail);
                return SyncResult.partialFailure(
                        "部分失败：成功 " + success + "/" + total + " 个工单",
                        errorDetail, totalBoardCount.get());
            } else {
                String errorDetail = String.join("; ", errors);
                log.error("批次 {} 同步全部失败，耗时: {}ms，失败明细: {}",
                        batchNum, elapsed, errorDetail);
                return SyncResult.failure(
                        "全部失败：" + total + " 个工单均同步失败", errorDetail);
            }

        } catch (Exception e) {
            log.error("批次 {} 同步异常: {}", batchNum, e.getMessage(), e);
            return SyncResult.failure("同步异常: " + e.getMessage(), e.getMessage());
        }
    }

    @Override
    public SyncResult discoverAndSyncByPartCode(String partCode) {
        return discoverAndSyncByPartCode(partCode, false);
    }

    @Override
    public SyncResult discoverAndResyncByPartCode(String partCode) {
        return discoverAndSyncByPartCode(partCode, true);
    }

    private SyncResult discoverAndSyncByPartCode(String partCode, boolean resync) {
        if (!syncEnabled) {
            log.debug("面板工序同步功能未开启，跳过板件 {} 的批次发现", partCode);
            return null;
        }
        if (partCode == null || partCode.trim().isEmpty()) {
            log.warn("板件码为空，无法发现批次");
            return null;
        }
        try {
            List<String> barcodes = java.util.Collections.singletonList(partCode);
            BatchQueryResponseDTO response = thirdPartyMesClient.batchQueryProcess(barcodes);
            if (response == null || response.getCode() == null || response.getCode() != 0
                    || response.getData() == null || response.getData().isEmpty()) {
                log.warn("MES batchQuery 未返回板件 {} 的批次信息", partCode);
                return null;
            }
            String batchNum = response.getData().stream()
                    .filter(item -> partCode.equals(item.getFtm()))
                    .findFirst()
                    .map(BatchQueryResponseDTO.BatchQueryItem::getFpjh)
                    .orElse(null);
            if (batchNum == null || batchNum.trim().isEmpty()) {
                log.warn("MES batchQuery 返回的板件 {} 缺少批次号", partCode);
                return null;
            }
            log.info("从 MES 发现板件 {} 属于批次 {}，触发{}全批次同步", partCode, batchNum,
                    resync ? "查询接口去重后的" : "");
            return resync ? resyncBatchProcess(batchNum) : syncBatchProcessIfNeeded(batchNum);
        } catch (Exception e) {
            log.error("从 MES 发现板件 {} 的批次失败: {}", partCode, e.getMessage());
            return null;
        }
    }

    private String buildFailureDetail(String batchNum, String workId, SyncPullResult pullResult) {
        String status = pullResult != null ? pullResult.getStatus() : null;
        String errorCode = pullResult != null ? pullResult.getErrorCode() : null;
        String errorMessage = pullResult != null ? pullResult.getErrorMessage() : null;
        MesWorkOrder latest = null;
        try {
            latest = workOrderMapper.selectByBatchNumAndWorkId(batchNum, workId);
            if (!hasText(errorMessage) && latest != null) {
                errorMessage = latest.getErrorMessage();
            }
        } catch (Exception e) {
            log.warn("查询工单 {} 最新失败原因失败: {}", workId, e.getMessage());
        }

        StringBuilder builder = new StringBuilder();
        builder.append("同步失败");
        builder.append(", status=").append(hasText(status) ? status : "UNKNOWN");
        if (hasText(errorCode)) {
            builder.append(", errorCode=").append(errorCode);
        }
        if (hasText(errorMessage)) {
            builder.append(", errorMessage=").append(errorMessage);
        } else {
            builder.append(", errorMessage=未返回错误信息，请查看同时间第三方接口调用日志");
        }
        if (latest != null) {
            builder.append(", dbStatus=").append(latest.getPrepackageStatus());
            builder.append(", retryCount=").append(latest.getRetryCount());
            builder.append(", lastPullTime=").append(latest.getLastPullTime());
        }
        return builder.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void insertRecord(String batchNum, String workId) {
        try {
            MesPanelProcessSync record = new MesPanelProcessSync();
            record.setBatchNum(batchNum);
            record.setWorkId(workId);
            record.setCreatedTime(LocalDateTime.now());
            panelProcessSyncMapper.insert(record);
        } catch (Exception e) {
            log.debug("工单 {} 同步记录已存在（并发插入），跳过", workId);
        }
    }

    private void updateRecord(String batchNum, String workId, String result, String errorDetail) {
        try {
            panelProcessSyncMapper.updateResult(batchNum, workId, result, errorDetail);
        } catch (Exception e) {
            log.warn("更新工单 {} 同步记录失败: {}", workId, e.getMessage());
        }
    }
}
