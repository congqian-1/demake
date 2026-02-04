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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 板件详细信息响应DTO
 *
 * 与 mes_part 表结构对齐，返回全量字段。
 *
 * @author MES Team
 */
@Data
@Schema(description = "板件详细信息响应")
public class PartDetailResponse {

    @Schema(description = "板件ID（数据库主键）")
    private Long id;

    @Schema(description = "板件码（唯一标识）")
    private String partCode;

    @Schema(description = "批次号（冗余）")
    private String batchNum;

    @Schema(description = "工单号（冗余）")
    private String workId;

    @Schema(description = "箱码ID（外键）")
    private Long boxId;

    @Schema(description = "包件ID（外键）")
    private Long packageId;

    @Schema(description = "第几层")
    private Integer layer;

    @Schema(description = "第几片")
    private Integer piece;

    @Schema(description = "板件ID（业务标识）")
    private String itemCode;

    @Schema(description = "板件描述")
    private String itemName;

    @Schema(description = "花色")
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

    @Schema(description = "标准码列表（解析后的Map列表）")
    private List<Map<String, Integer>> standardList;

    @Schema(description = "标准码原始JSON")
    private String standardListRaw;

    @Schema(description = "真实打包包号")
    private String realPackageNo;

    @Schema(description = "包件信息")
    @JsonProperty("package")
    private PackageSummary packageInfo;

    @Schema(description = "箱子信息")
    private BoxSummary box;

    @Schema(description = "预包装订单信息")
    private PrepackageOrderSummary prepackageOrder;

    @Schema(description = "工单信息")
    private WorkOrderSummary workOrder;

    @Schema(description = "优化文件信息")
    private OptimizingFileSummary optimizingFile;

    @Schema(description = "批次信息")
    private BatchSummary batch;

    @Schema(description = "逻辑删除标识（0-未删除、1-已删除）")
    private Integer isDeleted;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    @Schema(description = "板件描述（兼容字段）")
    private String description;

    @Schema(description = "花色（兼容字段）")
    private String color;
}
