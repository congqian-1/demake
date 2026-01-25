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

import com.tongzhou.mes.service1.pojo.dto.PartDetailResponse;
import com.tongzhou.mes.service1.pojo.dto.PartPackageResponse;
import com.tongzhou.mes.service1.pojo.dto.PartWorkOrderBatchResponse;

/**
 * 板件查询服务接口
 * 
 * @author MES Team
 */
public interface PartQueryService {

    /**
     * 根据板件码查询工单与批次信息
     * 
     * @param partCode 板件码
     * @return 工单、优化文件、批次信息
     * @throws com.tongzhou.mes.service1.exception.PartNotFoundException 板件码不存在
     * @throws com.tongzhou.mes.service1.exception.WorkOrderUpdatingException 工单数据更新中
     */
    PartWorkOrderBatchResponse queryWorkOrderAndBatch(String partCode);

    /**
     * 根据板件码查询包装数据
     * 
     * @param partCode 板件码
     * @return 箱码、订单、板件位置信息
     * @throws com.tongzhou.mes.service1.exception.PartNotFoundException 板件码不存在
     * @throws com.tongzhou.mes.service1.exception.WorkOrderUpdatingException 工单数据更新中
     */
    PartPackageResponse queryPackage(String partCode);

    /**
     * 根据板件码查询板件详细信息
     * 
     * @param partCode 板件码
     * @return 板件详细信息
     * @throws com.tongzhou.mes.service1.exception.PartNotFoundException 板件码不存在
     */
    PartDetailResponse queryDetail(String partCode);
}
