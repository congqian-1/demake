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

import com.tongzhou.mes.service1.pojo.dto.BatchPushRequest;

/**
 * 批次服务接口
 * 
 * @author MES Team
 */
public interface BatchService {

    /**
     * 保存批次数据（含工单和优化文件）
     * 实现幂等性：如果批次号已存在，则删除旧数据后重新插入
     * 
     * @param request 批次推送请求
     * @return 批次号
     */
    String saveBatch(BatchPushRequest request);
}
