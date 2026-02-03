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
import com.tongzhou.mes.service1.pojo.entity.MesPrepackageOrder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 预包装订单表Mapper接口
 * 
 * @author MES Team
 */
@Mapper
public interface MesPrepackageOrderMapper extends BaseMapper<MesPrepackageOrder> {

    /**
     * Select prepackage orders by work order ids.
     */
    @Select({"<script>",
        "SELECT * FROM mes_prepackage_order",
        "WHERE work_order_id IN",
        "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "AND is_deleted = 0",
        "</script>"})
    List<MesPrepackageOrder> selectByWorkOrderIds(@Param("workOrderIds") List<Long> workOrderIds);

    /**
     * Select prepackage order by order number.
     */
    @Select("SELECT * FROM mes_prepackage_order WHERE order_num = #{orderNum} AND is_deleted = 0")
    MesPrepackageOrder selectByOrderNum(@Param("orderNum") String orderNum);

    /**
     * Select prepackage order by work id.
     */
    @Select("SELECT * FROM mes_prepackage_order WHERE work_id = #{workId} AND is_deleted = 0")
    MesPrepackageOrder selectByWorkId(@Param("workId") String workId);

    /**
     * Physically delete prepackage orders by workId to avoid unique key conflicts during overwrite pulls.
     */
    @Delete("DELETE FROM mes_prepackage_order WHERE work_id = #{workId}")
    int physicalDeleteByWorkId(@Param("workId") String workId);
}
