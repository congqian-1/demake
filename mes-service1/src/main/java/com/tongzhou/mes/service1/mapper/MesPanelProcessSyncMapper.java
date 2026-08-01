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
import com.tongzhou.mes.service1.pojo.entity.MesPanelProcessSync;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 看板工序同步记录表Mapper接口
 *
 * @author MES Team
 */
@Mapper
public interface MesPanelProcessSyncMapper extends BaseMapper<MesPanelProcessSync> {

    /**
     * 查询批次下是否已有同步记录（用于去重判断）。
     */
    @Select("SELECT COUNT(1) FROM mes_panel_process_sync WHERE batch_num = #{batchNum} LIMIT 1")
    int countByBatchNum(@Param("batchNum") String batchNum);

    /**
     * 根据批次号和工单号查询单条记录。
     */
    @Select("SELECT * FROM mes_panel_process_sync WHERE batch_num = #{batchNum} AND work_id = #{workId}")
    MesPanelProcessSync selectByBatchNumAndWorkId(@Param("batchNum") String batchNum,
                                                   @Param("workId") String workId);

    /**
     * 更新同步结果和失败原因。
     */
    @Update("UPDATE mes_panel_process_sync SET sync_result = #{syncResult},"
            + " error_detail = #{errorDetail}, synced_at = NOW()"
            + " WHERE batch_num = #{batchNum} AND work_id = #{workId}")
    int updateResult(@Param("batchNum") String batchNum,
                     @Param("workId") String workId,
                     @Param("syncResult") String syncResult,
                     @Param("errorDetail") String errorDetail);
}
