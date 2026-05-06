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

package com.tongzhou.mes.service1.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 唯一约束错误中文映射
 */
public final class UniqueConstraintErrorMapper {

    public static final String FALLBACK_CODE = "DUPLICATE_DATA";
    public static final String FALLBACK_MESSAGE = "数据唯一性冲突：存在重复记录，新增失败";

    private static final Map<String, String> MESSAGE_MAP;
    private static final Map<String, String> CODE_MAP;

    static {
        Map<String, String> messages = new HashMap<>();
        messages.put("uk_batch_work", "工单重复：同一批次下工单号已存在，无法重复新增");
        messages.put("uk_batch_work_wo", "工单重复：同一批次下工单号已存在，无法重复新增");
        messages.put("uk_batch_work_box", "箱码重复：同一批次同一工单下箱码已存在，无法重复新增");
        messages.put("uk_batch_work_box_package", "包件重复：同一批次同一工单同一箱码下包号已存在，无法重复新增");
        messages.put("uk_part_code", "板件重复：板件编码已存在，无法重复新增");
        MESSAGE_MAP = Collections.unmodifiableMap(messages);

        Map<String, String> codes = new HashMap<>();
        codes.put("uk_batch_work", "DUP_WORK_ORDER");
        codes.put("uk_batch_work_wo", "DUP_WORK_ORDER");
        codes.put("uk_batch_work_box", "DUP_BOX_CODE");
        codes.put("uk_batch_work_box_package", "DUP_PACKAGE_NO");
        codes.put("uk_part_code", "DUP_PART_CODE");
        CODE_MAP = Collections.unmodifiableMap(codes);
    }

    private UniqueConstraintErrorMapper() {
    }

    public static MappedError map(Throwable throwable) {
        String raw = throwable == null ? "" : safeMessage(throwable).toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : MESSAGE_MAP.entrySet()) {
            if (raw.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                String key = entry.getKey();
                return new MappedError(
                    CODE_MAP.getOrDefault(key, FALLBACK_CODE),
                    entry.getValue(),
                    key
                );
            }
        }
        return new MappedError(FALLBACK_CODE, FALLBACK_MESSAGE, null);
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable.getMessage() != null) {
            return throwable.getMessage();
        }
        Throwable cause = throwable.getCause();
        while (cause != null) {
            if (cause.getMessage() != null) {
                return cause.getMessage();
            }
            cause = cause.getCause();
        }
        return "";
    }

    public static class MappedError {
        private final String errorCode;
        private final String errorMessage;
        private final String constraintName;

        public MappedError(String errorCode, String errorMessage, String constraintName) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.constraintName = constraintName;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getConstraintName() {
            return constraintName;
        }
    }
}
