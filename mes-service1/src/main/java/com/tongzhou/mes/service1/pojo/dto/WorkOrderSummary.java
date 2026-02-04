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

@Data
@Schema(description = "工单信息")
public class WorkOrderSummary {

    @Schema(description = "工单ID", example = "100")
    private Long id;

    @Schema(description = "批次ID", example = "1")
    private Long batchId;

    @Schema(description = "优化文件ID", example = "10")
    private Long optimizingFileId;

    @Schema(description = "工单号", example = "WO-001")
    private String workId;

    @Schema(description = "工艺路线", example = "LINE-A")
    private String route;

    @Schema(description = "工单类型", example = "STANDARD")
    private String orderType;

    @Schema(description = "预包装状态", example = "DONE")
    private String prepackageStatus;
}
