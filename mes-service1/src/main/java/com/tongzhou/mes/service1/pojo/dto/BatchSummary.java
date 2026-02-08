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
@Schema(description = "批次信息")
public class BatchSummary {

    @Schema(description = "批次ID", example = "1")
    private Long id;

    @Schema(description = "批次号", example = "BATCH-001")
    private String batchNum;

    @Schema(description = "批次类型", example = "1")
    private Integer batchType;

    @Schema(description = "生产时间")
    private LocalDateTime productTime;

    @Schema(description = "开料/排样时间")
    private LocalDateTime nestingTime;

    @Schema(description = "简易批次号")
    private String simpleBatchNum;

    @Schema(description = "线路/区域信息")
    private String ymba014;

    @Schema(description = "属性标识")
    private String ymba016;

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
