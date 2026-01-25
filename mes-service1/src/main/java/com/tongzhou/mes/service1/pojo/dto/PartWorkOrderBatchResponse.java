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

package com.tongzhou.mes.service1.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 板件码查询工单与批次信息响应DTO
 * 
 * @author MES Team
 */
@Data
@Schema(description = "板件码查询工单与批次信息响应")
public class PartWorkOrderBatchResponse {

    @Schema(description = "工单信息")
    private WorkOrderInfo workOrder;

    @Schema(description = "优化文件信息")
    private OptimizingFileInfo optimizingFile;

    @Schema(description = "批次信息")
    private BatchInfo batch;

    /**
     * 工单信息
     */
    @Data
    @Schema(description = "工单信息")
    public static class WorkOrderInfo {

        @Schema(description = "工单ID")
        private Long id;

        @Schema(description = "工单号")
        private String workId;

        @Schema(description = "批次ID")
        private Long batchId;

        @Schema(description = "优化文件ID")
        private Long optimizingFileId;

        @Schema(description = "批次号（冗余）")
        private String batchNum;

        @Schema(description = "线路")
        private String route;

        @Schema(description = "订单类型")
        private String orderType;

        @Schema(description = "预包装数据拉取状态")
        private String prepackageStatus;

        @Schema(description = "重试次数")
        private Integer retryCount;

        @Schema(description = "最后拉取时间")
        private LocalDateTime lastPullTime;

        @Schema(description = "错误信息")
        private String errorMessage;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

        @Schema(description = "更新时间")
        private LocalDateTime updatedTime;
    }

    /**
     * 优化文件信息
     */
    @Data
    @Schema(description = "优化文件信息")
    public static class OptimizingFileInfo {

        @Schema(description = "优化文件ID")
        private Long id;

        @Schema(description = "批次ID")
        private Long batchId;

        @Schema(description = "批次号（冗余）")
        private String batchNum;

        @Schema(description = "优化文件名称")
        private String optimizingFileName;

        @Schema(description = "工位编码")
        private String stationCode;

        @Schema(description = "是否加急（0=不加急/1=加急）")
        private Integer urgency;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

        @Schema(description = "更新时间")
        private LocalDateTime updatedTime;
    }

    /**
     * 批次信息
     */
    @Data
    @Schema(description = "批次信息")
    public static class BatchInfo {

        @Schema(description = "批次ID")
        private Long id;

        @Schema(description = "批次号")
        private String batchNum;

        @Schema(description = "批次类型（1=衣柜柜体/2=橱柜柜体/3=衣柜门板/4=橱柜门板/5=合并条码/6=补板）")
        private Integer batchType;

        @Schema(description = "生产日期")
        private LocalDate productTime;

        @Schema(description = "简易批次号")
        private String simpleBatchNum;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

        @Schema(description = "更新时间")
        private LocalDateTime updatedTime;
    }
}
