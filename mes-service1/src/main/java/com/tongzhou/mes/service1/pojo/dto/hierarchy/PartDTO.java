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

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Part DTO.
 */
@Data
public class PartDTO {
    private Long id;
    private Long packageId;
    private String partCode;
    private Integer layer;
    private Integer piece;
    private String itemCode;
    private String matName;
    private BigDecimal itemLength;
    private BigDecimal itemWidth;
    private BigDecimal itemDepth;
    private BigDecimal xAxis;
    private BigDecimal yAxis;
    private BigDecimal zAxis;
    private Integer sortOrder;
    private String standardList;
    private String realPackageNo;
    private List<WorkReportDTO> workReports = new ArrayList<>();
}
