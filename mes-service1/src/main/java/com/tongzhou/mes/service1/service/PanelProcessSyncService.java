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

package com.tongzhou.mes.service1.service;

/**
 * 看板工序同步服务接口
 *
 * @author MES Team
 */
public interface PanelProcessSyncService {

    /**
     * 同步指定批次下所有工单的板件工序信息。
     * 每个批次只同步一次（内存 + 数据库双重去重），重启后也不会重复。
     *
     * @param batchNum 批次号
     * @return 同步结果
     */
    SyncResult syncBatchProcessIfNeeded(String batchNum);

    /**
     * 根据板件码从 MES 发现批次号并触发全批次同步。
     * 用于本地没有该板件时，通过 MES batchQuery 反查批次后自动拉取数据。
     *
     * @param partCode 板件码
     * @return 同步结果，MES 查不到或同步未触发时返回 null
     */
    SyncResult discoverAndSyncByPartCode(String partCode);

    /**
     * 同步结果。
     */
    class SyncResult {
        /** 是否已经触发过同步（之前已同步过返回 true，本次新触发返回 false） */
        private final boolean alreadySynced;
        /** 本次同步是否成功 */
        private final boolean success;
        /** 结果描述 */
        private final String message;
        /** 详细错误信息（成功时为空） */
        private final String errorDetail;
        /** 更新的板件数量 */
        private final int updatedBoardCount;

        public SyncResult(boolean alreadySynced, boolean success, String message,
                          String errorDetail, int updatedBoardCount) {
            this.alreadySynced = alreadySynced;
            this.success = success;
            this.message = message;
            this.errorDetail = errorDetail;
            this.updatedBoardCount = updatedBoardCount;
        }

        public static SyncResult alreadySynced() {
            return new SyncResult(true, true, "该批次已同步过", null, 0);
        }

        public static SyncResult success(String message, int updatedBoardCount) {
            return new SyncResult(false, true, message, null, updatedBoardCount);
        }

        public static SyncResult failure(String message, String errorDetail) {
            return new SyncResult(false, false, message, errorDetail, 0);
        }

        public static SyncResult partialFailure(String message, String errorDetail,
                                                 int updatedBoardCount) {
            return new SyncResult(false, false, message, errorDetail, updatedBoardCount);
        }

        public boolean isAlreadySynced() { return alreadySynced; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorDetail() { return errorDetail; }
        public int getUpdatedBoardCount() { return updatedBoardCount; }
    }
}
