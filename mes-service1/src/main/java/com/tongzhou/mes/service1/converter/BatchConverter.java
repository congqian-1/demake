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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
        batch.setBatchType(parseInteger(request.getBatchType()));
        batch.setProductTime(parseDateTime(request.getProductTime()));
        batch.setNestingTime(parseDateTime(request.getNestingTime()));
        batch.setSimpleBatchNum(request.getSimpleBatchNum());
        batch.setYmba014(request.getYmba014());
        batch.setYmba016(request.getYmba016());
        
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
        file.setStationCode(defaultIfBlank(fileInfo.getStationCode(), "UNKNOWN"));
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
        workOrder.setRoute(defaultIfBlank(orderInfo.getRoute(), "/"));
        workOrder.setRouteId(orderInfo.getRouteId());
        workOrder.setOrderType(defaultIfBlank(orderInfo.getOrderType(), "UNKNOWN"));
        workOrder.setDeliveryTime(parseDateTime(orderInfo.getDeliveryTime()));
        workOrder.setNestingTime(parseDateTime(orderInfo.getNestingTime()));
        workOrder.setYmba014(orderInfo.getYmba014());
        workOrder.setYmba015(orderInfo.getYmba015());
        workOrder.setYmba016(orderInfo.getYmba016());
        workOrder.setPart0(normalizeNullString(orderInfo.getPart0()));
        workOrder.setCondition0(normalizeNullString(orderInfo.getCondition0()));
        workOrder.setPartTime0(parseDateTime(orderInfo.getPartTime0()));
        workOrder.setZuz(orderInfo.getZuz());
        workOrder.setPrepackageStatus("NOT_PULLED");
        workOrder.setRetryCount(0);
        
        return workOrder;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "NULL".equalsIgnoreCase(trimmed)) {
            return null;
        }

        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        for (int i = 0; i < formatters.length; i++) {
            DateTimeFormatter formatter = formatters[i];
            try {
                if (i == 2) {
                    LocalDate date = LocalDate.parse(trimmed, formatter);
                    return date.atStartOfDay();
                }
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String normalizeNullString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "NULL".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
