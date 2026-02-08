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
@Schema(description = "箱子信息")
public class BoxSummary {

    @Schema(description = "箱码ID", example = "2000")
    private Long id;

    @Schema(description = "预包装订单ID", example = "1000")
    private Long prepackageOrderId;

    @Schema(description = "批次号（冗余）")
    private String batchNum;

    @Schema(description = "工单号（冗余）")
    private String workId;

    @Schema(description = "箱码", example = "BOX-001")
    private String boxCode;

    @Schema(description = "楼栋", example = "1")
    private String building;

    @Schema(description = "单元", example = "1")
    private String house;

    @Schema(description = "房间", example = "101")
    private String room;

    @Schema(description = "第几套")
    private Integer setno;

    @Schema(description = "颜色")
    private String color;

    @Schema(description = "单元")
    private String unit;

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
