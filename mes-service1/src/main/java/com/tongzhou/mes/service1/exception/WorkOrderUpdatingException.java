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

package com.tongzhou.mes.service1.exception;

/**
 * 工单数据更新中异常
 * HTTP 409 Conflict
 * 
 * @author MES Team
 */
public class WorkOrderUpdatingException extends RuntimeException {

    private final String workId;

    public WorkOrderUpdatingException(String workId) {
        super(String.format("工单 %s 的数据正在更新中，请稍后重试", workId));
        this.workId = workId;
    }

    public WorkOrderUpdatingException(String workId, String message) {
        super(message);
        this.workId = workId;
    }

    public String getWorkId() {
        return workId;
    }
}
