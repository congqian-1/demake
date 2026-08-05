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
    @Schema(description = "面板工序同步结果")
    private SyncInfo sync;

    @Data
    @Schema(description = "面板工序同步信息")
    public static class SyncInfo {
        @Schema(description = "是否同步成功")
        private boolean success;
        @Schema(description = "同步结果消息")
        private String message;
        @Schema(description = "面向前端的错误摘要（成功时为空，完整失败明细见服务日志或同步记录）")
        private String errorDetail;
        @Schema(description = "更新的板件数量")
        private int updatedBoardCount;
    }
}
