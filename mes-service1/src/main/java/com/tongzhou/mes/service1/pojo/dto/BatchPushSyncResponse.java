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
import java.util.ArrayList;
import java.util.List;

/**
 * 同步批次推送响应DTO
 */
@Data
@Schema(description = "同步批次推送响应")
public class BatchPushSyncResponse {

    private boolean success;
    private String message;
    private String batchNum;

    private int totalWorkOrders;
    private int successCount;
    private int failedCount;
    private int processingCount;
    private long totalBoardCount;

    private LocalDateTime finishedAt;
    private List<WorkOrderResult> workOrders = new ArrayList<>();

    @Data
    public static class WorkOrderResult {
        private String workId;
        private String status;
        private long boardCount;
        private String errorCode;
        private String errorMessage;
    }
}
