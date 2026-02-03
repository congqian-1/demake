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

/**
 * Batch hierarchy response wrapper.
 */
@Data
@Schema(description = "批次层级响应")
public class ResultBatchHierarchy {
    @Schema(description = "业务码", example = "0")
    private String code;
    @Schema(description = "业务消息", example = "OK")
    private String message;
    @Schema(description = "批次层级数据")
    private BatchHierarchy data;
}
