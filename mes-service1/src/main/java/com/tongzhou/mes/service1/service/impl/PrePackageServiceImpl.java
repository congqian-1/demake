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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tongzhou.mes.service1.client.ThirdPartyMesClient;
import com.tongzhou.mes.service1.mapper.*;
import com.tongzhou.mes.service1.pojo.dto.PrepackageDataDTO;
import com.tongzhou.mes.service1.pojo.entity.*;
import com.tongzhou.mes.service1.service.EmailNotificationService;
import com.tongzhou.mes.service1.service.PrePackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预包装服务实现类
 *
 * @author MES Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrePackageServiceImpl implements PrePackageService {

    private final MesWorkOrderMapper workOrderMapper;
    private final MesPrepackageOrderMapper prepackageOrderMapper;
    private final MesBoxCodeMapper boxCodeMapper;
    private final MesPackageMapper packageMapper;
    private final MesBoardMapper boardMapper;
    private final MesCorrectionLogMapper correctionLogMapper;
    private final ThirdPartyMesClient thirdPartyMesClient;
    private final EmailNotificationService emailNotificationService;
    private static final int MAX_BATCH_SIZE = 50;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long[] RETRY_DELAYS = {1000, 2000, 4000}; // 1s, 2s, 4s

    @Override
    public void pullPendingWorkOrders() {
        // 查询状态为"NOT_PULLED"的工单，最多50个
        List<MesWorkOrder> pendingOrders = workOrderMapper.selectList(
                new LambdaQueryWrapper<MesWorkOrder>()
                        .eq(MesWorkOrder::getPrepackageStatus, "NOT_PULLED")
                        .last("LIMIT " + MAX_BATCH_SIZE));

        if (pendingOrders.isEmpty()) {
            log.debug("没有待拉取的工单");
            return;
        }

        log.info("开始拉取预包装数据，待处理工单数量: {}", pendingOrders.size());

        for (MesWorkOrder workOrder : pendingOrders) {
            try {
                pullSingleWorkOrder(workOrder);
            } catch (Exception e) {
                log.error("拉取工单 {} 的预包装数据失败: {}", workOrder.getWorkId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 拉取单个工单的预包装数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void pullSingleWorkOrder(MesWorkOrder workOrder) throws Exception {
        String workId = workOrder.getWorkId();
        String batchNum = workOrder.getBatchNum();

        log.info("开始拉取工单预包装数据，工单号: {}, 批次号: {}", workId, batchNum);

        // 更新工单状态为"更新中"
        workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                .set(MesWorkOrder::getPrepackageStatus, "UPDATING")
                .set(MesWorkOrder::getLastPullTime, LocalDateTime.now())
                .eq(MesWorkOrder::getId, workOrder.getId()));

        try {
            // 带重试的拉取
            PrepackageDataDTO data = pullWithRetry(batchNum, workId);
            logThirdPartyCallInfo(batchNum, workId, "PULL_SUCCESS");

            if (data == null || data.getPrePackageInfo() == null || isEmptyPrepackage(data)) {
                String diagnostic = buildDiagnosticMessage("NO_DATA", batchNum, workId);
                workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                        .set(MesWorkOrder::getPrepackageStatus, "NO_DATA")
                        .set(MesWorkOrder::getErrorMessage, diagnostic)
                        .set(MesWorkOrder::getLastPullTime, LocalDateTime.now())
                        .eq(MesWorkOrder::getId, workOrder.getId()));
                log.error("工单 {} 无预包装数据", workId);
                logThirdPartyCall(batchNum, workId, "NO_DATA");
                return;
            }

            savePrePackageDataWithOverwrite(workOrder, data);

            // 更新工单状态为"已拉取"
            workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                    .set(MesWorkOrder::getPrepackageStatus, "PULLED")
                    .set(MesWorkOrder::getLastPullTime, LocalDateTime.now())
                    .set(MesWorkOrder::getRetryCount, 0)
                    .set(MesWorkOrder::getErrorMessage, null)
                    .eq(MesWorkOrder::getId, workOrder.getId()));

            log.info("工单预包装数据拉取成功，工单号: {}", workId);

        } catch (Exception e) {
            log.error("工单预包装数据拉取失败，工单号: {}, 错误: {}", workId, e.getMessage(), e);
            logThirdPartyCall(batchNum, workId, "EXCEPTION: " + e.getMessage());
            String diagnostic = buildDiagnosticMessage("EXCEPTION: " + e.getMessage(), batchNum, workId);
            Integer retryCountOverride = e instanceof RetryExhaustedException
                ? ((RetryExhaustedException) e).getRetryCount()
                : null;
            handlePullFailure(workOrder, diagnostic, retryCountOverride);
            throw e;
        }
    }

    private boolean isEmptyPrepackage(PrepackageDataDTO data) {
        PrepackageDataDTO.PrePackageInfo info = data.getPrePackageInfo();
        if (info == null || info.getBoxInfoDetails() == null || info.getBoxInfoDetails().isEmpty()) {
            return true;
        }
        for (PrepackageDataDTO.BoxInfoDetail box : info.getBoxInfoDetails()) {
            if (box.getPackageInfos() == null || box.getPackageInfos().isEmpty()) {
                continue;
            }
            for (PrepackageDataDTO.PackageInfo pkg : box.getPackageInfos()) {
                if (pkg.getPartInfos() != null && !pkg.getPartInfos().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void logThirdPartyCall(String batchNum, String workId, String reason) {
        ThirdPartyMesClient.LastCallSnapshot snapshot = thirdPartyMesClient.getLastCallSnapshot();
        if (snapshot == null) {
            log.error("第三方调用记录缺失，批次号: {}, 工单号: {}, 原因: {}", batchNum, workId, reason);
            return;
        }
        log.error(
            "第三方接口调用信息，原因: {}，批次号: {}，工单号: {}，url: {}，status: {}，error: {}，request: {}，response: {}",
            reason,
            batchNum,
            workId,
            snapshot.getUrl(),
            snapshot.getHttpStatus(),
            snapshot.getErrorMessage(),
            snapshot.getRequestBody(),
            snapshot.getResponseBody()
        );
    }

    private void logThirdPartyCallInfo(String batchNum, String workId, String reason) {
        ThirdPartyMesClient.LastCallSnapshot snapshot = thirdPartyMesClient.getLastCallSnapshot();
        if (snapshot == null) {
            log.info("第三方调用记录缺失，批次号: {}, 工单号: {}, 原因: {}", batchNum, workId, reason);
            return;
        }
        log.info(
            "第三方接口调用信息，原因: {}，批次号: {}，工单号: {}，url: {}，status: {}，error: {}，request: {}，response: {}",
            reason,
            batchNum,
            workId,
            snapshot.getUrl(),
            snapshot.getHttpStatus(),
            snapshot.getErrorMessage(),
            truncate(snapshot.getRequestBody()),
            truncate(snapshot.getResponseBody())
        );
    }

    private String buildDiagnosticMessage(String reason, String batchNum, String workId) {
        ThirdPartyMesClient.LastCallSnapshot snapshot = thirdPartyMesClient.getLastCallSnapshot();
        if (snapshot == null) {
            return reason;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(reason).append(" | ");
        builder.append("batchNum=").append(batchNum).append(", workId=").append(workId).append(", ");
        builder.append("url=").append(snapshot.getUrl()).append(", ");
        builder.append("status=").append(snapshot.getHttpStatus()).append(", ");
        builder.append("error=").append(snapshot.getErrorMessage()).append(", ");
        builder.append("request=").append(truncate(snapshot.getRequestBody())).append(", ");
        builder.append("response=").append(truncate(snapshot.getResponseBody()));
        return builder.toString();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        int max = 2000;
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...(truncated)";
    }

    /**
     * 带重试机制的拉取
     */
    private PrepackageDataDTO pullWithRetry(String batchNum, String workId) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRY_COUNT; attempt++) {
            try {
                log.info("第 {} 次尝试拉取预包装数据，工单号: {}", attempt + 1, workId);
                PrepackageDataDTO data = thirdPartyMesClient.getPrepackageInfo(batchNum, workId);
                log.info("预包装数据拉取成功（尝试 {} 次），工单号: {}", attempt + 1, workId);
                return data;
            } catch (Exception e) {
                lastException = e;
                log.warn("第 {} 次拉取失败，工单号: {}, 错误: {}", attempt + 1, workId, e.getMessage());

                if (attempt < MAX_RETRY_COUNT - 1) {
                    try {
                        long delay = RETRY_DELAYS[attempt];
                        log.info("等待 {}ms 后重试...", delay);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", ie);
                    }
                }
            }
        }

        throw new RetryExhaustedException(MAX_RETRY_COUNT, lastException);
    }

    /**
     * 重试耗尽异常（用于标记本轮已达到最大重试次数）
     */
    private static class RetryExhaustedException extends RuntimeException {
        private final int retryCount;

        private RetryExhaustedException(int retryCount, Exception cause) {
            super("预包装数据拉取失败，已重试 " + retryCount + " 次", cause);
            this.retryCount = retryCount;
        }

        private int getRetryCount() {
            return retryCount;
        }
    }

    /**
     * 保存预包装数据（四层嵌套）
     */
    @Transactional(rollbackFor = Exception.class)
    public void savePrePackageData(MesWorkOrder workOrder, PrepackageDataDTO data) {
        savePrePackageDataInternal(workOrder, data, null);
    }

    /**
     * 保存预包装数据（支持覆盖模式下的板件upsert）
     */
    private void savePrePackageDataInternal(
        MesWorkOrder workOrder,
        PrepackageDataDTO data,
        Map<String, MesBoard> existingBoardsByPartCode
    ) {
        String batchNum = workOrder.getBatchNum();
        String workId = workOrder.getWorkId();

        log.info("开始保存预包装数据，工单号: {}", workId);

        // 1. 保存预包装订单
        MesPrepackageOrder order = new MesPrepackageOrder();
        order.setWorkOrderId(workOrder.getId());
        order.setBatchId(workOrder.getBatchId());
        order.setBatchNum(batchNum);
        order.setWorkId(workId);

        PrepackageDataDTO.PrePackageInfo info = data.getPrePackageInfo();
        order.setOrderNum(info.getOrderNum());
        order.setConsignor(info.getConsignor());
        order.setContractNo(info.getContractNo());
        order.setWorkNum(info.getWorkNum());
        order.setReceiver(info.getReceiver());
        order.setPhone(info.getPhone());
        order.setShipBatch(info.getShipBatch());
        order.setInstallAddress(info.getInstallAddress());
        order.setCustomer(info.getCustomer());
        order.setReceiveRegion(info.getReceiveRegion());
        order.setSpace(info.getSpace());
        order.setPackType(info.getPackType());
        order.setProductType(info.getProductType());
        order.setPrepackageInfoSize(info.getPrepackageInfoSize());
        order.setTotalSet(info.getTotalSet());
        order.setMaxPackageNo(info.getMaxPackageNo());
        order.setProductionNum(info.getProductionNum());
        order.setIsProject(info.getIsProject());
        order.setCustomerName(info.getCustomerName());
        order.setFnumber(info.getFnumber());
        order.setDob(info.getDob());
        order.setDetailedAddress(info.getDetailedAddress());

        prepackageOrderMapper.insert(order);
        Long orderId = order.getId();
        log.info("已保存预包装订单，ID: {}", orderId);

        // 2. 保存箱码及其下级数据
        if (info.getBoxInfoDetails() != null) {
            for (PrepackageDataDTO.BoxInfoDetail boxInfo : info.getBoxInfoDetails()) {
                // 保存箱码
                MesBoxCode box = new MesBoxCode();
                box.setPrepackageOrderId(orderId);
                box.setBatchNum(batchNum);
                box.setWorkId(workId);
                box.setBoxCode(boxInfo.getBoxCode());
                box.setBuilding(boxInfo.getBuilding());
                box.setHouse(boxInfo.getHouse());
                box.setRoom(boxInfo.getRoom());
                box.setSetno(boxInfo.getSetno());
                box.setColor(boxInfo.getColor());
                box.setUnit(boxInfo.getUnit());

                boxCodeMapper.insert(box);
                Long boxId = box.getId();
                log.info("已保存箱码: {}, ID: {}", boxInfo.getBoxCode(), boxId);

                // 3. 保存包件及其下级数据
                if (boxInfo.getPackageInfos() != null) {
                    for (PrepackageDataDTO.PackageInfo packageInfo : boxInfo.getPackageInfos()) {
                        // 保存包件
                        MesPackage pkg = new MesPackage();
                        pkg.setBoxId(boxId);
                        pkg.setBatchNum(batchNum);
                        pkg.setWorkId(workId);
                        pkg.setBoxCode(boxInfo.getBoxCode());
                        pkg.setPackageNo(packageInfo.getPackageNo());
                        pkg.setLength(packageInfo.getLength());
                        pkg.setWidth(packageInfo.getWidth());
                        pkg.setDepth(packageInfo.getDepth());
                        pkg.setWeight(packageInfo.getWeight());
                        pkg.setPartCount(packageInfo.getPartCount());
                        pkg.setBoxType(packageInfo.getBoxType());
                        pkg.setBoxType2(packageInfo.getBoxType2());

                        packageMapper.insert(pkg);
                        Long packageId = pkg.getId();
                        log.info("已保存包件: 箱码={}, 包号={}, ID={}", boxInfo.getBoxCode(), packageInfo.getPackageNo(), packageId);

                        // 4. 保存板件
                        if (packageInfo.getPartInfos() != null) {
                            for (PrepackageDataDTO.PartInfo partInfo : packageInfo.getPartInfos()) {
                                MesBoard existingBoard = existingBoardsByPartCode != null
                                    ? existingBoardsByPartCode.get(partInfo.getPartCode())
                                    : null;

                                MesBoard board = new MesBoard();
                                if (existingBoard != null) {
                                    board.setId(existingBoard.getId());
                                }
                                board.setPackageId(packageId);
                                board.setBoxId(boxId);
                                board.setBatchNum(batchNum);
                                board.setWorkId(workId);
                                board.setPartCode(partInfo.getPartCode());
                                board.setLayer(partInfo.getLayer());
                                board.setPiece(partInfo.getPiece());
                                board.setItemCode(partInfo.getItemCode());
                                board.setItemName(partInfo.getItemName());
                                board.setMatName(partInfo.getMatName());
                                board.setItemLength(partInfo.getItemLength());
                                board.setItemWidth(partInfo.getItemWidth());
                                board.setItemDepth(partInfo.getItemDepth());
                                board.setXAxis(partInfo.getXAxis());
                                board.setYAxis(partInfo.getYAxis());
                                board.setZAxis(partInfo.getZAxis());
                                board.setSortOrder(partInfo.getSortOrder());
                                board.setStandardCode(partInfo.getStandardCode());
                                board.setRotate(partInfo.getRotate());
                                board.setProcessCode(partInfo.getProcessCode());
                                board.setStandardList(partInfo.getStandardListJson());
                                board.setIsDeleted(0);
                                board.setUpdatedTime(LocalDateTime.now());

                                if (existingBoard != null) {
                                    // The board was just soft-deleted; revive it first so updateById will match.
                                    boardMapper.reviveById(existingBoard.getId());
                                    boardMapper.updateById(board);
                                    log.info("已更新板件: {}", partInfo.getPartCode());
                                } else {
                                    boardMapper.insert(board);
                                    log.info("已保存板件: {}", partInfo.getPartCode());
                                }
                            }
                        }
                    }
                }
            }
        }

        log.info("预包装数据保存完成，工单号: {}", workId);
    }

    /**
     * 保存预包装数据（覆盖模式 - 用于数据修正）
     * 软删除旧板件（保留报工记录），物理删除包件/箱码/订单，插入新数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void savePrePackageDataWithOverwrite(MesWorkOrder workOrder, PrepackageDataDTO data) {
        String workId = workOrder.getWorkId();
        Long workOrderId = workOrder.getId();

        log.info("开始覆盖保存预包装数据（数据修正模式），工单号: {}", workId);

        List<MesPrepackageOrder> existingOrders = prepackageOrderMapper.selectList(
            new LambdaQueryWrapper<MesPrepackageOrder>()
                .eq(MesPrepackageOrder::getWorkOrderId, workOrderId)
        );
        List<Long> existingOrderIds = collectPrepackageOrderIds(existingOrders);
        List<MesBoxCode> existingBoxes = existingOrderIds.isEmpty()
            ? new ArrayList<>()
            : boxCodeMapper.selectByPrepackageOrderIds(existingOrderIds);
        List<Long> existingBoxIds = collectBoxIds(existingBoxes);
        List<MesPackage> existingPackages = existingBoxIds.isEmpty()
            ? new ArrayList<>()
            : packageMapper.selectByBoxIds(existingBoxIds);
        List<Long> existingPackageIds = collectPackageIds(existingPackages);
        List<MesBoard> existingBoards = existingPackageIds.isEmpty()
            ? new ArrayList<>()
            : boardMapper.selectByPackageIds(existingPackageIds);
        Map<String, MesBoard> existingBoardsByPartCode = new HashMap<>();
        for (MesBoard board : existingBoards) {
            existingBoardsByPartCode.put(board.getPartCode(), board);
        }

        int deletedBoards = 0;
        if (!existingPackageIds.isEmpty()) {
            deletedBoards = boardMapper.update(null,
                new LambdaUpdateWrapper<MesBoard>()
                    .set(MesBoard::getIsDeleted, 1)
                    .set(MesBoard::getUpdatedTime, LocalDateTime.now())
                    .in(MesBoard::getPackageId, existingPackageIds)
            );
        }
        log.info("软删除旧板件数量: {}", deletedBoards);

        int deletedPackages = 0;
        if (!existingBoxIds.isEmpty()) {
            deletedPackages = packageMapper.physicalDeleteByBoxIds(existingBoxIds);
        }
        log.info("物理删除旧包件数量: {}", deletedPackages);

        int deletedBoxes = 0;
        if (!existingOrderIds.isEmpty()) {
            deletedBoxes = boxCodeMapper.physicalDeleteByPrepackageOrderIds(existingOrderIds);
        }
        log.info("物理删除旧箱码数量: {}", deletedBoxes);

        int deletedOrders = prepackageOrderMapper.physicalDeleteByWorkOrderId(workOrderId);
        log.info("物理删除旧预包装订单数量: {}", deletedOrders);

        // 5. 插入新的预包装数据（复用原有的保存逻辑）
        savePrePackageDataInternal(workOrder, data, existingBoardsByPartCode);

        log.info("预包装数据覆盖完成，工单号: {}", workId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void repullWorkOrder(String workId, String operator, String reason) {
        log.info("开始重新拉取工单预包装数据，工单号: {}, 操作人: {}, 原因: {}", workId, operator, reason);

        List<MesWorkOrder> workOrders = workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>()
                .eq(MesWorkOrder::getWorkId, workId)
                .eq(MesWorkOrder::getIsDeleted, 0)
        );
        if (workOrders.isEmpty()) {
            throw new RuntimeException("工单不存在: " + workId);
        }

        for (MesWorkOrder workOrder : workOrders) {
            MesCorrectionLog correctionLog = buildCorrectionLog(workOrder, operator, reason, "NOT_PULLED");
            correctionLogMapper.insert(correctionLog);
            workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                    .set(MesWorkOrder::getPrepackageStatus, "NOT_PULLED")
                    .set(MesWorkOrder::getRetryCount, 0)
                    .set(MesWorkOrder::getErrorMessage, null)
                    .eq(MesWorkOrder::getId, workOrder.getId()));
            correctionLog.setResult("SUCCESS");
            correctionLog.setUpdatedBy(operator);
            correctionLog.setUpdatedTime(LocalDateTime.now());
            correctionLogMapper.updateById(correctionLog);
        }

        log.info("已重置工单状态为未拉取，工单号: {}, 命中记录数: {}", workId, workOrders.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int repullBatchWorkOrders(String batchNum, String operator, String reason) {
        List<MesWorkOrder> workOrders = workOrderMapper.selectByBatchNum(batchNum);
        if (workOrders.isEmpty()) {
            throw new RuntimeException("批次不存在或批次下无工单: " + batchNum);
        }

        for (MesWorkOrder workOrder : workOrders) {
            MesCorrectionLog correctionLog = buildCorrectionLog(workOrder, operator, reason, "NOT_PULLED");
            correctionLogMapper.insert(correctionLog);
            workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                .set(MesWorkOrder::getPrepackageStatus, "NOT_PULLED")
                .set(MesWorkOrder::getRetryCount, 0)
                .set(MesWorkOrder::getErrorMessage, null)
                .eq(MesWorkOrder::getId, workOrder.getId()));
            correctionLog.setResult("SUCCESS");
            correctionLog.setUpdatedBy(operator);
            correctionLog.setUpdatedTime(LocalDateTime.now());
            correctionLogMapper.updateById(correctionLog);
        }

        log.info("已重置批次 {} 下全部工单为未拉取，数量: {}", batchNum, workOrders.size());
        return workOrders.size();
    }

    private MesCorrectionLog buildCorrectionLog(MesWorkOrder workOrder, String operator, String reason, String newStatus) {
        MesCorrectionLog correctionLog = new MesCorrectionLog();
        correctionLog.setWorkOrderId(workOrder.getId());
        correctionLog.setWorkId(workOrder.getWorkId());
        correctionLog.setOperator(operator);
        correctionLog.setOperationTime(LocalDateTime.now());
        correctionLog.setCorrectionReason(reason);
        correctionLog.setOldStatus(workOrder.getPrepackageStatus());
        correctionLog.setNewStatus(newStatus);
        correctionLog.setPartCountBefore(0);
        correctionLog.setPartCountAfter(0);
        correctionLog.setCreatedBy(operator);
        correctionLog.setCreatedTime(LocalDateTime.now());
        return correctionLog;
    }

    private List<Long> collectPrepackageOrderIds(List<MesPrepackageOrder> entities) {
        List<Long> ids = new ArrayList<>();
        for (MesPrepackageOrder entity : entities) {
            if (entity.getId() != null) {
                ids.add(entity.getId());
            }
        }
        return ids;
    }

    private List<Long> collectBoxIds(List<MesBoxCode> entities) {
        List<Long> ids = new ArrayList<>();
        for (MesBoxCode entity : entities) {
            if (entity.getId() != null) {
                ids.add(entity.getId());
            }
        }
        return ids;
    }

    private List<Long> collectPackageIds(List<MesPackage> entities) {
        List<Long> ids = new ArrayList<>();
        for (MesPackage entity : entities) {
            if (entity.getId() != null) {
                ids.add(entity.getId());
            }
        }
        return ids;
    }

    /**
     * 处理拉取失败
     */
    private void handlePullFailure(MesWorkOrder workOrder, String errorMessage, Integer retryCountOverride) {
        int newRetryCount = retryCountOverride != null
            ? retryCountOverride
            : (workOrder.getRetryCount() != null ? workOrder.getRetryCount() : 0) + 1;
        String workId = workOrder.getWorkId();
        String batchNum = workOrder.getBatchNum();

        log.warn("工单 {} 拉取失败，重试次数: {}/{}", workId, newRetryCount, MAX_RETRY_COUNT);

        if (newRetryCount >= MAX_RETRY_COUNT) {
            // 达到最大重试次数，标记为失败
            workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                    .set(MesWorkOrder::getPrepackageStatus, "FAILED")
                    .set(MesWorkOrder::getRetryCount, newRetryCount)
                    .set(MesWorkOrder::getErrorMessage, errorMessage)
                    .set(MesWorkOrder::getLastPullTime, LocalDateTime.now())
                    .eq(MesWorkOrder::getId, workOrder.getId()));

            // 发送邮件通知
            try {
                emailNotificationService.sendPrepackagePullFailureNotification(
                        batchNum, workId, errorMessage, newRetryCount);
                log.info("已发送拉取失败邮件通知，工单号: {}", workId);
            } catch (Exception e) {
                log.error("发送邮件通知失败: {}", e.getMessage(), e);
            }

            log.error("工单 {} 已达到最大重试次数，标记为拉取失败", workId);
        } else {
            // 更新重试次数，状态恢复为未拉取
            workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                    .set(MesWorkOrder::getPrepackageStatus, "NOT_PULLED")
                    .set(MesWorkOrder::getRetryCount, newRetryCount)
                    .set(MesWorkOrder::getErrorMessage, errorMessage)
                    .set(MesWorkOrder::getLastPullTime, LocalDateTime.now())
                    .eq(MesWorkOrder::getId, workOrder.getId()));
        }
    }
}
