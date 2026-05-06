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

import com.tongzhou.mes.service1.pojo.dto.BatchPushSyncRequest;
import com.tongzhou.mes.service1.pojo.dto.BatchPushSyncResponse;

/**
 * 同步批次推送服务
 */
public interface BatchSyncService {

    /**
     * 推送批次并同步拉取本次请求工单数据，直接返回处理结果。
     */
    BatchPushSyncResponse pushAndSync(BatchPushSyncRequest request);
}
