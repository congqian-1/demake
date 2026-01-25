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

package com.tongzhou.mes.service1.converter;

import com.tongzhou.mes.service1.pojo.dto.BatchPushRequest;
import com.tongzhou.mes.service1.pojo.entity.MesBatch;
import com.tongzhou.mes.service1.pojo.entity.MesOptimizationFile;
import com.tongzhou.mes.service1.pojo.entity.MesWorkOrder;
import org.springframework.stereotype.Component;

/**
 * 批次数据转换器
 */
@Component
public class BatchConverter {

    /**
     * 将批次推送请求转换为批次实体
     */
    public MesBatch toMesBatch(BatchPushRequest request) {
        if (request == null) {
            return null;
        }

        MesBatch batch = new MesBatch();
        batch.setBatchNum(request.getBatchNum());
        batch.setBatchType(request.getBatchType());
        batch.setProductTime(request.getProductTime());
        batch.setSimpleBatchNum(request.getSimpleBatchNum());
        
        return batch;
    }

    /**
     * 将优化文件信息转换为优化文件实体
     */
    public MesOptimizationFile toMesOptimizationFile(BatchPushRequest.OptimizingFileInfo fileInfo, String batchNum, Long batchId) {
        if (fileInfo == null) {
            return null;
        }

        MesOptimizationFile file = new MesOptimizationFile();
        file.setBatchId(batchId);
        file.setBatchNum(batchNum);
        file.setOptimizingFileName(fileInfo.getOptimizingFileName());
        file.setStationCode(fileInfo.getStationCode());
        file.setUrgency(fileInfo.getUrgency() != null ? fileInfo.getUrgency() : 0);
        
        return file;
    }

    /**
     * 将工单信息转换为工单实体
     */
    public MesWorkOrder toMesWorkOrder(BatchPushRequest.WorkOrderInfo orderInfo, String batchNum, Long batchId, Long optimizingFileId) {
        if (orderInfo == null) {
            return null;
        }

        MesWorkOrder workOrder = new MesWorkOrder();
        workOrder.setBatchId(batchId);
        workOrder.setOptimizingFileId(optimizingFileId);
        workOrder.setBatchNum(batchNum);
        workOrder.setWorkId(orderInfo.getWorkId());
        workOrder.setRoute(orderInfo.getRoute());
        workOrder.setOrderType(orderInfo.getOrderType());
        workOrder.setPrepackageStatus("NOT_PULLED");
        workOrder.setRetryCount(0);
        
        return workOrder;
    }
}
