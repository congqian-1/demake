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

package com.tongzhou.mes.service1.pojo.dto.hierarchy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Part DTO.
 */
@Data
@Schema(description = "板件信息")
public class PartDTO {
    @Schema(description = "板件ID", example = "4000")
    private Long id;
    @Schema(description = "包件ID", example = "3000")
    private Long packageId;
    @Schema(description = "箱码ID（冗余）", example = "2000")
    private Long boxId;
    @Schema(description = "批次号（冗余）", example = "BATCH-001")
    private String batchNum;
    @Schema(description = "工单号（冗余）", example = "WO-001")
    private String workId;
    @Schema(description = "板件码", example = "PART-001")
    private String partCode;
    @Schema(description = "第几层", example = "1")
    private Integer layer;
    @Schema(description = "第几片", example = "1")
    private Integer piece;
    @Schema(description = "板件ID（业务标识）", example = "ITEM-001")
    private String itemCode;
    @Schema(description = "板件描述")
    private String itemName;
    @Schema(description = "花色", example = "WHITE")
    private String matName;
    @Schema(description = "板件长")
    private String itemLength;
    @Schema(description = "板件宽")
    private String itemWidth;
    @Schema(description = "板件高")
    private String itemDepth;
    @Schema(description = "X轴坐标")
    private String xAxis;
    @Schema(description = "Y轴坐标")
    private String yAxis;
    @Schema(description = "Z轴坐标")
    private String zAxis;
    @Schema(description = "分拣顺序")
    private Integer sortOrder;
    @Schema(description = "标准码")
    private String standardCode;
    @Schema(description = "旋转字段")
    private String rotate;
    @Schema(description = "工艺代码")
    private String processCode;
    @Schema(description = "标准码原始JSON")
    private String standardList;
    @Schema(description = "真实打包包号", example = "PKG-REAL-001")
    private String realPackageNo;
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
    @Schema(description = "报工记录")
    private List<WorkReportDTO> workReports = new ArrayList<>();
}
