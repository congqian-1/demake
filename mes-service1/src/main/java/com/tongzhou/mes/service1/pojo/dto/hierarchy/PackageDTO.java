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
 * Package DTO.
 */
@Data
@Schema(description = "包件信息")
public class PackageDTO {
    @Schema(description = "包件ID", example = "3000")
    private Long id;
    @Schema(description = "箱码ID", example = "2000")
    private Long boxId;
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
    @Schema(description = "箱型", example = "A")
    private String boxType;
    @Schema(description = "板件列表")
    private List<PartDTO> parts = new ArrayList<>();
}
