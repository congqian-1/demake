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
 * 预包装服务接口
 * 
 * @author MES Team
 */
public interface PrePackageService {

    /**
     * 拉取所有待处理的工单的预包装数据
     * 每次最多处理50个工单
     */
    void pullPendingWorkOrders();

    /**
     * 重新拉取工单预包装数据（用于数据修正）
     * 
     * @param workId 工单号
     * @param operator 操作人
     * @param reason 修正原因
     */
    void repullWorkOrder(String workId, String operator, String reason);
}
