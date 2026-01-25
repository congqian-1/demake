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

package com.tongzhou.mes.service1.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tongzhou.mes.service1.pojo.entity.MesWorkOrder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 工单表Mapper接口
 * 
 * @author MES Team
 */
@Mapper
public interface MesWorkOrderMapper extends BaseMapper<MesWorkOrder> {
    
    /**
     * 物理删除指定批次的所有工单
     */
    @Delete("DELETE FROM mes_work_order WHERE batch_id = #{batchId}")
    int physicalDeleteByBatchId(@Param("batchId") Long batchId);
}
