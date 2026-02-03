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

import java.math.BigDecimal;
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
    @Schema(description = "板件码", example = "PART-001")
    private String partCode;
    @Schema(description = "第几层", example = "1")
    private Integer layer;
    @Schema(description = "第几片", example = "1")
    private Integer piece;
    @Schema(description = "板件ID（业务标识）", example = "ITEM-001")
    private String itemCode;
    @Schema(description = "花色", example = "WHITE")
    private String matName;
    @Schema(description = "板件长")
    private BigDecimal itemLength;
    @Schema(description = "板件宽")
    private BigDecimal itemWidth;
    @Schema(description = "板件高")
    private BigDecimal itemDepth;
    @Schema(description = "X轴坐标")
    private BigDecimal xAxis;
    @Schema(description = "Y轴坐标")
    private BigDecimal yAxis;
    @Schema(description = "Z轴坐标")
    private BigDecimal zAxis;
    @Schema(description = "分拣顺序")
    private Integer sortOrder;
    @Schema(description = "标准码原始JSON")
    private String standardList;
    @Schema(description = "真实打包包号", example = "PKG-REAL-001")
    private String realPackageNo;
    @Schema(description = "报工记录")
    private List<WorkReportDTO> workReports = new ArrayList<>();
}
