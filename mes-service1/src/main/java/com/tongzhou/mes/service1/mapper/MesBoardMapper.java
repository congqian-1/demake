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
import com.tongzhou.mes.service1.pojo.entity.MesBoard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 板件表Mapper接口
 * 
 * @author MES Team
 */
@Mapper
public interface MesBoardMapper extends BaseMapper<MesBoard> {

    /**
     * Select boards by package ids.
     */
    @Select({"<script>",
        "SELECT * FROM mes_part",
        "WHERE package_id IN",
        "<foreach collection='packageIds' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "AND is_deleted = 0",
        "</script>"})
    List<MesBoard> selectByPackageIds(@Param("packageIds") List<Long> packageIds);

    /**
     * Revive a logically deleted board so that subsequent updateById can succeed.
     */
    @Update("UPDATE mes_part SET is_deleted = 0 WHERE id = #{id}")
    int reviveById(@Param("id") Long id);

    /**
     * Count logically deleted boards by workId (bypasses MyBatis-Plus logic delete filter).
     */
    @Select("SELECT COUNT(1) FROM mes_part WHERE work_id = #{workId} AND is_deleted = 1")
    long countDeletedByWorkId(@Param("workId") String workId);
}
