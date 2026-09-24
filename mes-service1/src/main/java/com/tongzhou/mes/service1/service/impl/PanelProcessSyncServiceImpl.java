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
import java.util.UUID;
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

    private static final int SYNC_CORE_POOL_SIZE = 2;
    private static final int SYNC_MAX_POOL_SIZE = 4;
    private static final String BATCH_SYNC_MARKER = "__BATCH__";
    private static final long SYNC_PROCESSING_TIMEOUT_MINUTES = 30L;
    private static final int BATCH_ERROR_DETAIL_MAX_LENGTH = 1000;
    private static final String BATCH_TERMINAL_FALLBACK_DETAIL =
            "批次同步已结束，但结果详情写入失败，详见工单级记录";

    private final MesWorkOrderMapper workOrderMapper;
    private final ThirdPartyMesClient thirdPartyMesClient;
    private final MesPanelProcessSyncMapper panelProcessSyncMapper;
    private final PrePackageService prePackageService;

    @Value("${mes.panel.process.sync.enabled:true}")
    private boolean syncEnabled;

    /**
     * 同步专用线程池，并行调用 pullSingleWorkOrderForSync 以提升大批次性能。
     * 并发控制在 2~4，避免同批次大量工单同时覆盖写库引发死锁。
     * 调用方通过 CompletableFuture.allOf().join() 阻塞等待全部完成，
     * 对外表现为同步执行。
     */
    private final ExecutorService syncExecutor = new ThreadPoolExecutor(
            SYNC_CORE_POOL_SIZE, SYNC_MAX_POOL_SIZE, 60L, TimeUnit.SECONDS,
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

        String ownerToken = UUID.randomUUID().toString();
        SyncResult claimResult = claimBatchSync(batchNum, ownerToken);
        if (claimResult != null) {
            return claimResult;
        }

        return executeClaimedBatchSync(batchNum, PullMode.NORMAL, ownerToken);
    }

    @Override
    public SyncResult resyncBatchProcess(String batchNum) {
        if (!syncEnabled) {
            return SyncResult.failure("功能未开启", "mes.panel.process.sync.enabled=false");
        }
        if (batchNum == null || batchNum.trim().isEmpty()) {
            return SyncResult.failure("批次号为空", null);
        }

        String ownerToken = UUID.randomUUID().toString();
        SyncResult claimResult = claimBatchSync(batchNum, ownerToken);
        if (claimResult != null) {
            return claimResult;
        }

        try {
            log.info("批次 {} 首次由查询接口触发同步，按原保存逻辑重新拉取", batchNum);
            return executeClaimedBatchSync(batchNum, PullMode.RESYNC, ownerToken);
        } catch (Exception e) {
            log.error("批次 {} 查询接口触发同步异常: {}", batchNum, e.getMessage(), e);
            return SyncResult.failure("查询接口触发同步异常: " + e.getMessage(), e.getMessage());
        }
    }

    /**
     * 通过 (batch_num, __BATCH__) 唯一键原子抢占批次同步。
     * 返回 null 表示当前线程抢占成功；返回 SyncResult 表示应直接结束本次调用。
     */
    private SyncResult claimBatchSync(String batchNum, String ownerToken) {
        MesPanelProcessSync marker = panelProcessSyncMapper.selectByBatchNumAndWorkId(batchNum, BATCH_SYNC_MARKER);
        if (marker != null) {
            if (isProcessing(marker)) {
                LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(SYNC_PROCESSING_TIMEOUT_MINUTES);
                if (!isStale(marker, staleBefore)
                        || panelProcessSyncMapper.reclaimStaleBatchMarker(batchNum, staleBefore, ownerToken) == 0) {
                    log.info("批次 {} 正在同步数据中，拒绝本次查询", batchNum);
                    return SyncResult.syncing();
                }
                log.warn("批次 {} 的同步状态已超时，重新接管同步", batchNum);
                return null;
            }
            log.info("批次 {} 已由查询接口触发过同步，本次跳过第三方重拉", batchNum);
            return SyncResult.alreadySynced();
        }

        int existingRecordCount = panelProcessSyncMapper.countByBatchNum(batchNum);
        if (existingRecordCount > 0) {
            MesPanelProcessSync legacyProcessing = panelProcessSyncMapper.selectLatestLegacyProcessing(batchNum);
            if (legacyProcessing == null) {
                log.info("批次 {} 已有历史同步结果，本次跳过第三方重拉", batchNum);
                return SyncResult.alreadySynced();
            }
            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(SYNC_PROCESSING_TIMEOUT_MINUTES);
            if (!isStale(legacyProcessing, staleBefore)) {
                log.info("批次 {} 正在由旧版本同步数据中，拒绝本次查询", batchNum);
                return SyncResult.syncing();
            }
            log.warn("批次 {} 存在超时的旧版同步记录，重新发起同步", batchNum);
        }

        try {
            MesPanelProcessSync batchMarker = new MesPanelProcessSync();
            batchMarker.setBatchNum(batchNum);
            batchMarker.setWorkId(BATCH_SYNC_MARKER);
            batchMarker.setSyncResult("PROCESSING");
            batchMarker.setErrorDetail(ownerToken);
            batchMarker.setCreatedTime(LocalDateTime.now());
            panelProcessSyncMapper.insert(batchMarker);
            return null;
        } catch (Exception e) {
            MesPanelProcessSync concurrentMarker =
                    panelProcessSyncMapper.selectByBatchNumAndWorkId(batchNum, BATCH_SYNC_MARKER);
            if (concurrentMarker != null) {
                log.info("批次 {} 已被其他请求抢占，正在同步数据中", batchNum);
                return SyncResult.syncing();
            }
            throw e;
        }
    }

    private boolean isProcessing(MesPanelProcessSync record) {
        return record.getSyncResult() == null || "PROCESSING".equals(record.getSyncResult());
    }

    private boolean isStale(MesPanelProcessSync record, LocalDateTime staleBefore) {
        return record.getCreatedTime() == null || record.getCreatedTime().isBefore(staleBefore);
    }

    private SyncResult executeClaimedBatchSync(String batchNum, PullMode pullMode, String ownerToken) {
        SyncResult result = syncBatchProcess(batchNum, pullMode, ownerToken);
        if (result.isSyncing()) {
            try {
                panelProcessSyncMapper.releaseBatchMarkerForRetry(batchNum, ownerToken,
                        LocalDateTime.now().minusMinutes(SYNC_PROCESSING_TIMEOUT_MINUTES + 1));
            } catch (Exception e) {
                log.warn("批次 {} 释放同步租约失败，将继续按同步中返回: {}", batchNum, e.getMessage());
            }
            return result;
        }
        String terminalStatus = result.isSuccess() ? "SUCCESS" : "FAILED";
        String errorDetail = truncateBatchErrorDetail(result.getErrorDetail());
        try {
            int updated = panelProcessSyncMapper.updateBatchResult(batchNum,
                    terminalStatus, errorDetail, ownerToken);
            if (updated > 0) {
                return result;
            }
            log.warn("批次 {} 同步租约已被其他执行者接管，拒绝返回本次旧执行结果", batchNum);
        } catch (Exception e) {
            log.error("批次 {} 写入同步终态失败，改用短错误信息标记为 FAILED: {}", batchNum, e.getMessage());
            try {
                int fallbackUpdated = panelProcessSyncMapper.updateBatchResult(batchNum,
                        "FAILED", BATCH_TERMINAL_FALLBACK_DETAIL, ownerToken);
                if (fallbackUpdated > 0) {
                    return SyncResult.failure("批次同步结果写入失败", BATCH_TERMINAL_FALLBACK_DETAIL);
                }
                log.warn("批次 {} 写入 FAILED 兜底状态时租约已被其他执行者接管", batchNum);
            } catch (Exception fallbackException) {
                log.error("批次 {} 写入 FAILED 兜底状态仍失败: {}", batchNum, fallbackException.getMessage(),
                        fallbackException);
            }
        }
        return SyncResult.syncing();
    }

    private SyncResult syncBatchProcess(String batchNum, PullMode pullMode, String ownerToken) {
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
            AtomicInteger processingCount = new AtomicInteger(0);
            AtomicInteger totalBoardCount = new AtomicInteger(0);

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
                            log.info("批次 {} 工单 {} 仍在同步中", batchNum, workId);
                            processingCount.incrementAndGet();
                        } else {
                            String errMsg = buildFailureDetail(batchNum, workId, pullResult);
                            log.warn("批次 {} 工单 {} 同步失败: {}", batchNum, workId, errMsg);
                            updateRecord(batchNum, workId, "FAILED", errMsg);
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        String errMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
                        log.error("同步工单 {} 失败: {}", workId, errMsg, e);
                        updateRecord(batchNum, workId, "FAILED", errMsg);
                        failCount.incrementAndGet();
                    } finally {
                        refreshBatchMarkerLease(batchNum, ownerToken);
                    }
                }, syncExecutor);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            long elapsed = System.currentTimeMillis() - startTime;
            int total = workOrders.size();
            int success = successCount.get();
            int failed = failCount.get();

            if (processingCount.get() > 0) {
                log.info("批次 {} 仍在同步数据中，处理中工单: {}", batchNum, processingCount.get());
                return SyncResult.syncing();
            }

            if (failed == 0) {
                log.info("批次 {} 同步全部成功，工单: {}, 板件: {}, 耗时: {}ms",
                        batchNum, total, totalBoardCount.get(), elapsed);
                return SyncResult.success(
                        "同步完成，" + total + " 个工单全部成功，共 " + totalBoardCount.get() + " 个板件",
                        totalBoardCount.get());
            } else if (success > 0) {
                String errorDetail = buildBatchErrorSummary(success, failed, total);
                log.warn("批次 {} 同步部分失败，成功: {}, 失败: {}, 耗时: {}ms，详见工单级记录",
                        batchNum, success, failed, elapsed);
                return SyncResult.partialFailure(
                        "部分失败：成功 " + success + "/" + total + " 个工单",
                        errorDetail, totalBoardCount.get());
            } else {
                String errorDetail = buildBatchErrorSummary(success, failed, total);
                log.error("批次 {} 同步全部失败，耗时: {}ms，详见工单级记录",
                        batchNum, elapsed);
                return SyncResult.failure(
                        "全部失败：" + total + " 个工单均同步失败", errorDetail);
            }

        } catch (Exception e) {
            log.error("批次 {} 同步异常: {}", batchNum, e.getMessage(), e);
            return SyncResult.failure("同步异常: " + e.getMessage(),
                    truncateBatchErrorDetail("批次同步异常：" + e.getMessage() + "，详见服务日志和工单级记录"));
        }
    }

    private String buildBatchErrorSummary(int success, int failed, int total) {
        return truncateBatchErrorDetail(
                "成功 " + success + "/" + total + "，失败 " + failed + "，详见工单级记录");
    }

    private String truncateBatchErrorDetail(String errorDetail) {
        if (errorDetail == null || errorDetail.length() <= BATCH_ERROR_DETAIL_MAX_LENGTH) {
            return errorDetail;
        }
        String suffix = "...(已截断)";
        return errorDetail.substring(0, BATCH_ERROR_DETAIL_MAX_LENGTH - suffix.length()) + suffix;
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
            record.setSyncResult("PROCESSING");
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

    private void refreshBatchMarkerLease(String batchNum, String ownerToken) {
        try {
            panelProcessSyncMapper.refreshBatchMarkerLease(batchNum, ownerToken);
        } catch (Exception e) {
            log.warn("批次 {} 同步租约续期失败: {}", batchNum, e.getMessage());
        }
    }
}
