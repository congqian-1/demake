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
import com.tongzhou.mes.service1.pojo.entity.MesPackage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 包件表Mapper接口
 * 
 * @author MES Team
 */
@Mapper
public interface MesPackageMapper extends BaseMapper<MesPackage> {

    /**
     * Physically delete packages by workId during overwrite pulls.
     */
    @Delete("DELETE FROM mes_package WHERE work_id = #{workId}")
    int physicalDeleteByWorkId(@Param("workId") String workId);
}
