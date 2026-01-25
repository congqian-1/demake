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
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

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

        // 检查是否为重新拉取（已有预包装数据）
        boolean isRepull = checkIfRepull(workOrder);

        // 更新工单状态为"更新中"
        workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                .set(MesWorkOrder::getPrepackageStatus, "UPDATING")
                .set(MesWorkOrder::getLastPullTime, LocalDateTime.now())
                .eq(MesWorkOrder::getId, workOrder.getId()));

        try {
            // 带重试的拉取
            PrepackageDataDTO data = pullWithRetry(batchNum, workId);

            if (data == null || data.getPrePackageInfo() == null) {
                // 无预包装数据
                workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                        .set(MesWorkOrder::getPrepackageStatus, "NO_DATA")
                        .eq(MesWorkOrder::getId, workOrder.getId()));
                log.error("工单 {} 无预包装数据", workId);
                return;
            }

            // 保存预包装数据（根据是否重新拉取选择保存方式）
            if (isRepull) {
                savePrePackageDataWithOverwrite(workOrder, data);
            } else {
                savePrePackageData(workOrder, data);
            }

            // 更新工单状态为"已拉取"
            workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                    .set(MesWorkOrder::getPrepackageStatus, "PULLED")
                    .set(MesWorkOrder::getLastPullTime, LocalDateTime.now())
                    .set(MesWorkOrder::getRetryCount, 0)
                    .set(MesWorkOrder::getErrorMessage, null)
                    .eq(MesWorkOrder::getId, workOrder.getId()));

            log.info("工单预包装数据拉取成功，工单号: {}, 重新拉取: {}", workId, isRepull);

        } catch (Exception e) {
            log.error("工单预包装数据拉取失败，工单号: {}, 错误: {}", workId, e.getMessage(), e);
            Integer retryCountOverride = e instanceof RetryExhaustedException
                ? ((RetryExhaustedException) e).getRetryCount()
                : null;
            handlePullFailure(workOrder, e.getMessage(), retryCountOverride);
            throw e;
        }
    }

    /**
     * 检查是否为重新拉取
     */
    private boolean checkIfRepull(MesWorkOrder workOrder) {
        Long count = prepackageOrderMapper.selectCount(
            new LambdaQueryWrapper<MesPrepackageOrder>()
                .eq(MesPrepackageOrder::getWorkOrderId, workOrder.getId())
        );
        return count != null && count > 0;
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

        log.info("开始覆盖保存预包装数据（数据修正模式），工单号: {}", workId);

        // 0. 查询现有板件（覆盖模式下需要按partCode复用ID，避免唯一键冲突）
        List<MesBoard> existingBoards = boardMapper.selectList(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
        );
        Map<String, MesBoard> existingBoardsByPartCode = new HashMap<>();
        for (MesBoard board : existingBoards) {
            existingBoardsByPartCode.put(board.getPartCode(), board);
        }

        // 1. 软删除旧板件（设置is_deleted=1，保留报工记录关联）
        int deletedBoards = boardMapper.update(null,
            new LambdaUpdateWrapper<MesBoard>()
                .set(MesBoard::getIsDeleted, 1)
                .set(MesBoard::getUpdatedTime, LocalDateTime.now())
                .eq(MesBoard::getWorkId, workId)
        );
        log.info("软删除旧板件数量: {}", deletedBoards);

        // 2. 物理删除旧包件
        int deletedPackages = packageMapper.physicalDeleteByWorkId(workId);
        log.info("物理删除旧包件数量: {}", deletedPackages);

        // 3. 物理删除旧箱码
        int deletedBoxes = boxCodeMapper.physicalDeleteByWorkId(workId);
        log.info("物理删除旧箱码数量: {}", deletedBoxes);

        // 4. 物理删除旧预包装订单
        int deletedOrders = prepackageOrderMapper.physicalDeleteByWorkId(workId);
        log.info("物理删除旧预包装订单数量: {}", deletedOrders);

        // 5. 插入新的预包装数据（复用原有的保存逻辑）
        savePrePackageDataInternal(workOrder, data, existingBoardsByPartCode);

        log.info("预包装数据覆盖完成，工单号: {}", workId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void repullWorkOrder(String workId, String operator, String reason) {
        log.info("开始重新拉取工单预包装数据，工单号: {}, 操作人: {}, 原因: {}", workId, operator, reason);

        // 1. 查询工单
        MesWorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<MesWorkOrder>()
                .eq(MesWorkOrder::getWorkId, workId)
        );

        if (workOrder == null) {
            throw new RuntimeException("工单不存在: " + workId);
        }

        // 2. 记录修正前数据（统计数量）
        Long oldBoardCount = boardMapper.selectCount(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0)
        );

        Long oldPackageCount = packageMapper.selectCount(
            new LambdaQueryWrapper<MesPackage>()
                .eq(MesPackage::getWorkId, workId)
        );

        // 3. 创建修正日志
        MesCorrectionLog correctionLog = new MesCorrectionLog();
        correctionLog.setWorkOrderId(workOrder.getId());
        correctionLog.setWorkId(workId);
        correctionLog.setOperator(operator);
        correctionLog.setOperationTime(LocalDateTime.now());
        correctionLog.setCorrectionReason(reason);
        correctionLog.setOldStatus("PULLED");
        correctionLog.setNewStatus("REPULLING");
        correctionLog.setPartCountBefore(oldBoardCount.intValue());
        correctionLog.setPartCountAfter(0);
        correctionLog.setCreatedBy(operator);
        correctionLog.setCreatedTime(LocalDateTime.now());
        correctionLogMapper.insert(correctionLog);
        log.info("已创建修正日志，ID: {}", correctionLog.getId());

        try {
            // 4. 重置工单状态为"未拉取"
            workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                    .set(MesWorkOrder::getPrepackageStatus, "NOT_PULLED")
                    .set(MesWorkOrder::getRetryCount, 0)
                    .set(MesWorkOrder::getErrorMessage, null)
                    .eq(MesWorkOrder::getId, workOrder.getId()));

            log.info("已重置工单状态为未拉取，工单号: {}", workId);

            // 5. 立即触发拉取（会自动检测到是重新拉取，使用覆盖模式）
            pullSingleWorkOrder(workOrder);

            // 6. 记录修正后数据
            Long newBoardCount = boardMapper.selectCount(
                new LambdaQueryWrapper<MesBoard>()
                    .eq(MesBoard::getWorkId, workId)
                    .eq(MesBoard::getIsDeleted, 0)
            );

            Long newPackageCount = packageMapper.selectCount(
                new LambdaQueryWrapper<MesPackage>()
                    .eq(MesPackage::getWorkId, workId)
            );

            // 更新修正日志
            correctionLog.setNewStatus("PULLED");
            correctionLog.setPartCountAfter(newBoardCount.intValue());
            correctionLog.setResult("SUCCESS");
            correctionLog.setErrorMessage(null);
            correctionLog.setUpdatedBy(operator);
            correctionLog.setUpdatedTime(LocalDateTime.now());
            correctionLogMapper.updateById(correctionLog);

            log.info("工单数据修正完成，工单号: {}, 板件数变化: {} -> {}, 包件数变化: {} -> {}",
                workId, oldBoardCount, newBoardCount, oldPackageCount, newPackageCount);

        } catch (Exception e) {
            log.error("工单数据修正失败，工单号: {}, 错误: {}", workId, e.getMessage(), e);

            // 更新修正日志为失败（记录错误）
            correctionLog.setResult("FAILED");
            correctionLog.setErrorMessage(e.getMessage());
            correctionLog.setUpdatedBy(operator);
            correctionLog.setUpdatedTime(LocalDateTime.now());
            correctionLogMapper.updateById(correctionLog);

            throw new RuntimeException("工单数据修正失败: " + e.getMessage(), e);
        }
    }

    /**
     * 修正数据快照（用于JSON序列化）
     */
    private static class CorrectionDataSnapshot {
        public Long boardCount;
        public Long packageCount;

        public CorrectionDataSnapshot(Long boardCount, Long packageCount) {
            this.boardCount = boardCount;
            this.packageCount = packageCount;
        }
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
