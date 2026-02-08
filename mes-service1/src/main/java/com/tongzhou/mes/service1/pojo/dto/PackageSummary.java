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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "包件信息")
public class PackageSummary {

    @Schema(description = "包件ID", example = "3000")
    private Long id;

    @Schema(description = "箱码ID", example = "2000")
    private Long boxId;

    @Schema(description = "批次号（冗余）")
    private String batchNum;

    @Schema(description = "工单号（冗余）")
    private String workId;

    @Schema(description = "箱码（冗余）")
    private String boxCode;

    @Schema(description = "包号", example = "1")
    private Integer packageNo;

    @Schema(description = "长")
    private BigDecimal length;

    @Schema(description = "宽")
    private BigDecimal width;

    @Schema(description = "高")
    private BigDecimal depth;

    @Schema(description = "重量")
    private BigDecimal weight;

    @Schema(description = "部件数")
    private Integer partCount;

    @Schema(description = "箱型", example = "A")
    private String boxType;

    @Schema(description = "箱型（二级）")
    private String boxType2;

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
