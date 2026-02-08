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

@Data
@Schema(description = "工单信息")
public class WorkOrderSummary {

    @Schema(description = "工单ID", example = "100")
    private Long id;

    @Schema(description = "批次ID", example = "1")
    private Long batchId;

    @Schema(description = "优化文件ID", example = "10")
    private Long optimizingFileId;

    @Schema(description = "批次号（冗余）")
    private String batchNum;

    @Schema(description = "工单号", example = "WO-001")
    private String workId;

    @Schema(description = "工艺路线", example = "LINE-A")
    private String route;

    @Schema(description = "线路ID")
    private String routeId;

    @Schema(description = "工单类型", example = "STANDARD")
    private String orderType;

    @Schema(description = "交付日期")
    private LocalDateTime deliveryTime;

    @Schema(description = "开料/排样时间")
    private LocalDateTime nestingTime;

    @Schema(description = "线路/区域信息")
    private String ymba014;

    @Schema(description = "工位/区域信息")
    private String ymba015;

    @Schema(description = "属性标识")
    private String ymba016;

    @Schema(description = "部件字段")
    private String part0;

    @Schema(description = "条件字段")
    private String condition0;

    @Schema(description = "部件时间字段")
    private LocalDateTime partTime0;

    @Schema(description = "组/套标记")
    private Integer zuz;

    @Schema(description = "预包装状态", example = "DONE")
    private String prepackageStatus;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "最后拉取时间")
    private LocalDateTime lastPullTime;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "逻辑删除标识（0-未删除、1-已删除）")
    private Integer isDeleted;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新人")
    private String updatedBy;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
