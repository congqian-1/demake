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

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tongzhou.mes.service1.converter.BatchConverter;
import com.tongzhou.mes.service1.mapper.MesBatchMapper;
import com.tongzhou.mes.service1.mapper.MesOptimizationFileMapper;
import com.tongzhou.mes.service1.mapper.MesWorkOrderMapper;
import com.tongzhou.mes.service1.pojo.bo.BatchSaveResult;
import com.tongzhou.mes.service1.pojo.dto.BatchPushRequest;
import com.tongzhou.mes.service1.pojo.entity.MesBatch;
import com.tongzhou.mes.service1.pojo.entity.MesOptimizationFile;
import com.tongzhou.mes.service1.pojo.entity.MesWorkOrder;
import com.tongzhou.mes.service1.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 批次服务实现类
 *
 * @author MES Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private final MesBatchMapper batchMapper;
    private final MesOptimizationFileMapper optimizationFileMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final BatchConverter batchConverter;

    /**
     * 保存批次数据：同批次内支持增量补推，重复组合只重置状态不新增记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveBatch(BatchPushRequest request) {
        return saveBatchWithResult(request).getBatchNum();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchSaveResult saveBatchWithResult(BatchPushRequest request) {
        String batchNum = request.getBatchNum();
        log.info("开始处理批次推送，批次号: {}", batchNum);

        MesBatch batch = batchMapper.selectByBatchNum(batchNum);
        if (batch == null) {
            batch = batchConverter.toMesBatch(request);
            batchMapper.insert(batch);
            log.info("已创建新批次: {}, ID: {}", batchNum, batch.getId());
        } else {
            mergeBatch(batch, request);
            batchMapper.updateById(batch);
            log.info("已复用批次并刷新信息: {}, ID: {}", batchNum, batch.getId());
        }

        BatchSaveResult saveResult = new BatchSaveResult();
        saveResult.setBatchNum(batchNum);
        Long batchId = batch.getId();
        int totalWorkOrders = 0;
        for (BatchPushRequest.OptimizingFileInfo fileInfo : request.getOptimizingFiles()) {
            MesOptimizationFile file = optimizationFileMapper.selectByBatchIdAndFileName(batchId, fileInfo.getOptimizingFileName());
            if (file == null) {
                file = batchConverter.toMesOptimizationFile(fileInfo, batchNum, batchId);
                optimizationFileMapper.insert(file);
                log.info("已新增优化文件: {}, ID: {}", fileInfo.getOptimizingFileName(), file.getId());
            } else {
                file.setStationCode(fileInfo.getStationCode());
                file.setUrgency(fileInfo.getUrgency());
                optimizationFileMapper.updateById(file);
                optimizationFileMapper.touchById(file.getId());
                log.info("已复用优化文件并刷新信息: {}, ID: {}", fileInfo.getOptimizingFileName(), file.getId());
            }

            for (BatchPushRequest.WorkOrderInfo orderInfo : fileInfo.getWorkOrders()) {
                MesWorkOrder existingWorkOrder = workOrderMapper.selectByBatchNumAndWorkId(batchNum, orderInfo.getWorkId());
                if (existingWorkOrder == null) {
                    MesWorkOrder workOrder = batchConverter.toMesWorkOrder(orderInfo, batchNum, batchId, file.getId());
                    workOrder.setReprocessPending(0);
                    workOrderMapper.insert(workOrder);
                    log.info("已新增工单: {}, 工单号: {}", workOrder.getId(), orderInfo.getWorkId());
                } else {
                    mergeWorkOrder(existingWorkOrder, orderInfo, batchNum, batchId, file.getId());
                    // 先写公共字段
                    workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                        .set(MesWorkOrder::getBatchId, existingWorkOrder.getBatchId())
                        .set(MesWorkOrder::getOptimizingFileId, existingWorkOrder.getOptimizingFileId())
                        .set(MesWorkOrder::getBatchNum, existingWorkOrder.getBatchNum())
                        .set(MesWorkOrder::getRoute, existingWorkOrder.getRoute())
                        .set(MesWorkOrder::getRouteId, existingWorkOrder.getRouteId())
                        .set(MesWorkOrder::getOrderType, existingWorkOrder.getOrderType())
                        .set(MesWorkOrder::getDeliveryTime, existingWorkOrder.getDeliveryTime())
                        .set(MesWorkOrder::getNestingTime, existingWorkOrder.getNestingTime())
                        .set(MesWorkOrder::getYmba014, existingWorkOrder.getYmba014())
                        .set(MesWorkOrder::getYmba015, existingWorkOrder.getYmba015())
                        .set(MesWorkOrder::getYmba016, existingWorkOrder.getYmba016())
                        .set(MesWorkOrder::getPart0, existingWorkOrder.getPart0())
                        .set(MesWorkOrder::getCondition0, existingWorkOrder.getCondition0())
                        .set(MesWorkOrder::getPartTime0, existingWorkOrder.getPartTime0())
                        .set(MesWorkOrder::getZuz, existingWorkOrder.getZuz())
                        .eq(MesWorkOrder::getId, existingWorkOrder.getId()));

                    // 共存语义：UPDATING期间不打断当前执行，仅记录挂起重拉
                    int updatedToNotPulled = workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                        .set(MesWorkOrder::getPrepackageStatus, "NOT_PULLED")
                        .set(MesWorkOrder::getRetryCount, 0)
                        .set(MesWorkOrder::getErrorMessage, null)
                        .set(MesWorkOrder::getReprocessPending, 0)
                        .eq(MesWorkOrder::getId, existingWorkOrder.getId())
                        .ne(MesWorkOrder::getPrepackageStatus, "UPDATING"));
                    if (updatedToNotPulled == 0) {
                        workOrderMapper.update(null, new LambdaUpdateWrapper<MesWorkOrder>()
                            .set(MesWorkOrder::getRetryCount, 0)
                            .set(MesWorkOrder::getErrorMessage, null)
                            .set(MesWorkOrder::getReprocessPending, 1)
                            .eq(MesWorkOrder::getId, existingWorkOrder.getId()));
                    }
                    log.info("已复用工单并重置状态: {}, 工单号: {}", existingWorkOrder.getId(), orderInfo.getWorkId());
                }
                saveResult.getTargetedWorkIds().add(orderInfo.getWorkId());
                totalWorkOrders++;
            }
        }

        log.info("批次推送处理完成，批次号: {}, 优化文件数: {}, 工单数: {}", 
                batchNum, request.getOptimizingFiles().size(), totalWorkOrders);
        
        return saveResult;
    }

    private void mergeBatch(MesBatch batch, BatchPushRequest request) {
        MesBatch updatedBatch = batchConverter.toMesBatch(request);
        batch.setBatchType(updatedBatch.getBatchType());
        batch.setProductTime(updatedBatch.getProductTime());
        batch.setNestingTime(updatedBatch.getNestingTime());
        batch.setSimpleBatchNum(updatedBatch.getSimpleBatchNum());
        batch.setYmba014(updatedBatch.getYmba014());
        batch.setYmba016(updatedBatch.getYmba016());
    }

    private void mergeWorkOrder(MesWorkOrder workOrder,
                                BatchPushRequest.WorkOrderInfo orderInfo,
                                String batchNum,
                                Long batchId,
                                Long optimizingFileId) {
        MesWorkOrder incoming = batchConverter.toMesWorkOrder(orderInfo, batchNum, batchId, optimizingFileId);
        workOrder.setBatchId(batchId);
        workOrder.setOptimizingFileId(optimizingFileId);
        workOrder.setBatchNum(batchNum);
        workOrder.setRoute(incoming.getRoute());
        workOrder.setRouteId(incoming.getRouteId());
        workOrder.setOrderType(incoming.getOrderType());
        workOrder.setDeliveryTime(incoming.getDeliveryTime());
        workOrder.setNestingTime(incoming.getNestingTime());
        workOrder.setYmba014(incoming.getYmba014());
        workOrder.setYmba015(incoming.getYmba015());
        workOrder.setYmba016(incoming.getYmba016());
        workOrder.setPart0(incoming.getPart0());
        workOrder.setCondition0(incoming.getCondition0());
        workOrder.setPartTime0(incoming.getPartTime0());
        workOrder.setZuz(incoming.getZuz());
        workOrder.setPrepackageStatus("NOT_PULLED");
        workOrder.setRetryCount(0);
        workOrder.setErrorMessage(null);
        workOrder.setReprocessPending(0);
    }
}
