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
 * 板件码不存在异常
 * HTTP 404 Not Found
 * 
 * @author MES Team
 */
public class PartNotFoundException extends RuntimeException {

    private final String partCode;

    public PartNotFoundException(String partCode) {
        super(String.format("板件码 %s 不存在", partCode));
        this.partCode = partCode;
    }

    public PartNotFoundException(String partCode, String message) {
        super(message);
        this.partCode = partCode;
    }

    public String getPartCode() {
        return partCode;
    }
}
