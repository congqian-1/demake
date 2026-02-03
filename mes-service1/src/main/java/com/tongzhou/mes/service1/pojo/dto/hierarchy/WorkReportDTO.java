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

/**
 * Work report DTO.
 */
@Data
@Schema(description = "板件报工记录")
public class WorkReportDTO {
    @Schema(description = "报工ID", example = "5000")
    private Long id;
    @Schema(description = "板件码", example = "PART-001")
    private String partCode;
    @Schema(description = "板件状态", example = "DONE")
    private String partStatus;
    @Schema(description = "工位编码", example = "C1A001")
    private String stationCode;
    @Schema(description = "报工时间")
    private LocalDateTime reportTime;
}
