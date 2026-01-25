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

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 批次推送请求DTO
 * 
 * @author MES Team
 */
@Data
@Schema(description = "批次推送请求")
public class BatchPushRequest {

    @NotBlank(message = "批次号不能为空")
    @Schema(description = "批次号", required = true, example = "BATCH-20240125-001")
    private String batchNum;

    @NotNull(message = "批次类型不能为空")
    @Schema(description = "批次类型（1=衣柜柜体/2=橱柜柜体/3=衣柜门板/4=橱柜门板/5=合并条码/6=补板）", required = true, example = "1")
    private Integer batchType;

    @NotNull(message = "生产日期不能为空")
    @Schema(description = "生产日期", required = true)
    private LocalDate productTime;

    @Schema(description = "简易批次号")
    private String simpleBatchNum;

    @Valid
    @NotEmpty(message = "优化文件列表不能为空")
    @Schema(description = "优化文件列表", required = true)
    private List<OptimizingFileInfo> optimizingFiles;

    /**
     * 优化文件信息
     */
    @Data
    @Schema(description = "优化文件信息")
    public static class OptimizingFileInfo {

        @NotBlank(message = "优化文件名称不能为空")
        @Schema(description = "优化文件名称", required = true, example = "OPT-20240125-001.txt")
        private String optimizingFileName;

        @NotBlank(message = "工位编码不能为空")
        @Schema(description = "工位编码（C1A001/C1A002/CMA001/CMA002/YMA001/YMA002）", required = true, example = "C1A001")
        private String stationCode;

        @Schema(description = "是否加急（0=不加急/1=加急）", example = "0")
        private Integer urgency;

        @Valid
        @NotEmpty(message = "工单列表不能为空")
        @Schema(description = "工单列表", required = true)
        private List<WorkOrderInfo> workOrders;
    }

    /**
     * 工单信息
     */
    @Data
    @Schema(description = "工单信息")
    public static class WorkOrderInfo {

        @NotBlank(message = "工单号不能为空")
        @Schema(description = "工单号", required = true, example = "WO-20240125-001")
        private String workId;

        @NotBlank(message = "线路不能为空")
        @Schema(description = "线路", required = true, example = "LINE-A")
        private String route;

        @NotBlank(message = "订单类型不能为空")
        @Schema(description = "订单类型", required = true, example = "STANDARD")
        private String orderType;
    }
}
