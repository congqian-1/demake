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
 * 重复报工异常
 * 当板件在同一状态重复报工时抛出此异常
 *
 * @author MES Team
 */
public class DuplicateWorkReportException extends RuntimeException {

    private final String partCode;
    private final String partStatus;

    public DuplicateWorkReportException(String partCode, String partStatus) {
        super(String.format("板件[%s]已在状态[%s]报工，不可重复提交", partCode, partStatus));
        this.partCode = partCode;
        this.partStatus = partStatus;
    }

    public String getPartCode() {
        return partCode;
    }

    public String getPartStatus() {
        return partStatus;
    }
}
