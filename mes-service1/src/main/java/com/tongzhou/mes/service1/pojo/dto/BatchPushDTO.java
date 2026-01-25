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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次推送DTO（接收第三方MES推送的批次数据）
 * 
 * @author MES Team
 */
@Data
@Schema(description = "批次推送数据")
public class BatchPushDTO {

    @Schema(description = "批次号", required = true)
    private String batchNo;

    @Schema(description = "批次描述")
    private String batchDescription;

    @Schema(description = "批次状态")
    private String batchStatus;

    @Schema(description = "批次推送时间", required = true)
    private LocalDateTime batchPushTime;

    @Schema(description = "工单列表")
    private List<WorkOrderDTO> workOrders;

    @Schema(description = "优化文件列表")
    private List<OptimizationFileDTO> optimizationFiles;

    @Schema(description = "备注")
    private String remark;

    /**
     * 工单DTO
     */
    @Data
    @Schema(description = "工单数据")
    public static class WorkOrderDTO {

        @Schema(description = "工单号", required = true)
        private String workOrderNo;

        @Schema(description = "客户名称")
        private String customerName;

        @Schema(description = "产品型号")
        private String productModel;

        @Schema(description = "订单数量")
        private Integer orderQuantity;

        @Schema(description = "计划开工日期")
        private LocalDateTime plannedStartDate;

        @Schema(description = "计划完工日期")
        private LocalDateTime plannedEndDate;

        @Schema(description = "工单状态")
        private String workOrderStatus;

        @Schema(description = "工单优先级")
        private String priority;

        @Schema(description = "备注")
        private String remark;
    }

    /**
     * 优化文件DTO
     */
    @Data
    @Schema(description = "优化文件数据")
    public static class OptimizationFileDTO {

        @Schema(description = "文件名", required = true)
        private String fileName;

        @Schema(description = "文件路径", required = true)
        private String filePath;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Schema(description = "文件MD5校验码")
        private String fileMd5;

        @Schema(description = "文件上传时间")
        private LocalDateTime fileUploadTime;

        @Schema(description = "备注")
        private String remark;
    }
}
