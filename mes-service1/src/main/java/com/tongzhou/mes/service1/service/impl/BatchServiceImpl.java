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
import com.tongzhou.mes.service1.converter.BatchConverter;
import com.tongzhou.mes.service1.mapper.MesBatchMapper;
import com.tongzhou.mes.service1.mapper.MesOptimizationFileMapper;
import com.tongzhou.mes.service1.mapper.MesWorkOrderMapper;
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
     * 保存批次数据（含幂等性处理）
     * 
     * 逻辑：
     * 1. 检查批次号是否已存在
     * 2. 如果存在，先删除旧的优化文件和工单数据
     * 3. 保存/更新批次信息
     * 4. 保存优化文件和工单数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveBatch(BatchPushRequest request) {
        String batchNum = request.getBatchNum();
        log.info("开始处理批次推送，批次号: {}", batchNum);

        // 1. 查询批次是否已存在
        MesBatch existingBatch = batchMapper.selectOne(
                new LambdaQueryWrapper<MesBatch>()
                        .eq(MesBatch::getBatchNum, batchNum)
        );

        Long batchId;
        if (existingBatch != null) {
            log.info("批次号 {} 已存在，进行幂等性处理：物理删除旧数据并重新插入", batchNum);
            batchId = existingBatch.getId();

            // 先物理删除旧的工单数据（有外键约束，需要先删）
            int deletedWorkOrders = workOrderMapper.physicalDeleteByBatchId(batchId);
            log.info("已物理删除批次 {} 的旧工单数据，数量: {}", batchNum, deletedWorkOrders);

            // 再物理删除旧的优化文件数据
            int deletedFiles = optimizationFileMapper.physicalDeleteByBatchId(batchId);
            log.info("已物理删除批次 {} 的旧优化文件数据，数量: {}", batchNum, deletedFiles);

            // 更新批次信息
            MesBatch updatedBatch = batchConverter.toMesBatch(request);
            existingBatch.setBatchType(updatedBatch.getBatchType());
            existingBatch.setProductTime(updatedBatch.getProductTime());
            existingBatch.setNestingTime(updatedBatch.getNestingTime());
            existingBatch.setSimpleBatchNum(updatedBatch.getSimpleBatchNum());
            existingBatch.setYmba014(updatedBatch.getYmba014());
            existingBatch.setYmba016(updatedBatch.getYmba016());
            batchMapper.updateById(existingBatch);
            log.info("已更新批次信息: {}", batchNum);
        } else {
            // 新批次，直接插入
            MesBatch batch = batchConverter.toMesBatch(request);
            batchMapper.insert(batch);
            batchId = batch.getId();
            log.info("已创建新批次: {}, ID: {}", batchNum, batchId);
        }

        // 2. 保存优化文件和工单数据
        int totalWorkOrders = 0;
        for (BatchPushRequest.OptimizingFileInfo fileInfo : request.getOptimizingFiles()) {
            // 保存优化文件
            MesOptimizationFile file = batchConverter.toMesOptimizationFile(fileInfo, batchNum, batchId);
            optimizationFileMapper.insert(file);
            Long optimizingFileId = file.getId();
            log.info("已保存优化文件: {}, ID: {}", fileInfo.getOptimizingFileName(), optimizingFileId);

            // 保存该优化文件下的所有工单
            for (BatchPushRequest.WorkOrderInfo orderInfo : fileInfo.getWorkOrders()) {
                MesWorkOrder workOrder = batchConverter.toMesWorkOrder(orderInfo, batchNum, batchId, optimizingFileId);
                workOrderMapper.insert(workOrder);
                totalWorkOrders++;
                log.info("已保存工单: {}, 工单号: {}", workOrder.getId(), orderInfo.getWorkId());
            }
        }

        log.info("批次推送处理完成，批次号: {}, 优化文件数: {}, 工单数: {}", 
                batchNum, request.getOptimizingFiles().size(), totalWorkOrders);
        
        return batchNum;
    }
}
