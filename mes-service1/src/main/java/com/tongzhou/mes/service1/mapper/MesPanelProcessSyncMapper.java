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

import java.time.LocalDateTime;

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
     * 查询批次下最近一条旧版同步中记录（旧版本未写批次级占位记录）。
     */
    @Select("SELECT * FROM mes_panel_process_sync"
            + " WHERE batch_num = #{batchNum} AND work_id != '__BATCH__'"
            + " AND (sync_result IS NULL OR sync_result = 'PROCESSING')"
            + " ORDER BY created_time DESC LIMIT 1")
    MesPanelProcessSync selectLatestLegacyProcessing(@Param("batchNum") String batchNum);

    /**
     * 原子接管超时的批次级同步占位记录。
     */
    @Update("UPDATE mes_panel_process_sync SET sync_result = 'PROCESSING',"
            + " error_detail = #{ownerToken}, synced_at = NULL, created_time = NOW()"
            + " WHERE batch_num = #{batchNum} AND work_id = '__BATCH__'"
            + " AND (sync_result IS NULL OR sync_result = 'PROCESSING')"
            + " AND (created_time IS NULL OR created_time < #{staleBefore})")
    int reclaimStaleBatchMarker(@Param("batchNum") String batchNum,
                                @Param("staleBefore") LocalDateTime staleBefore,
                                @Param("ownerToken") String ownerToken);

    /**
     * 当前执行者续租批次级同步占位记录。
     */
    @Update("UPDATE mes_panel_process_sync SET created_time = NOW()"
            + " WHERE batch_num = #{batchNum} AND work_id = '__BATCH__'"
            + " AND sync_result = 'PROCESSING' AND error_detail = #{ownerToken}")
    int refreshBatchMarkerLease(@Param("batchNum") String batchNum,
                                @Param("ownerToken") String ownerToken);

    /**
     * 外部工单仍在处理时释放本次租约，允许下次查询立即重新探测。
     */
    @Update("UPDATE mes_panel_process_sync SET created_time = #{retryBefore}"
            + " WHERE batch_num = #{batchNum} AND work_id = '__BATCH__'"
            + " AND sync_result = 'PROCESSING' AND error_detail = #{ownerToken}")
    int releaseBatchMarkerForRetry(@Param("batchNum") String batchNum,
                                   @Param("ownerToken") String ownerToken,
                                   @Param("retryBefore") LocalDateTime retryBefore);

    /**
     * 仅允许当前租约持有者写入批次终态，防止超时接管后的旧执行者覆盖新状态。
     */
    @Update("UPDATE mes_panel_process_sync SET sync_result = #{syncResult},"
            + " error_detail = #{errorDetail}, synced_at = NOW()"
            + " WHERE batch_num = #{batchNum} AND work_id = '__BATCH__'"
            + " AND sync_result = 'PROCESSING' AND error_detail = #{ownerToken}")
    int updateBatchResult(@Param("batchNum") String batchNum,
                          @Param("syncResult") String syncResult,
                          @Param("errorDetail") String errorDetail,
                          @Param("ownerToken") String ownerToken);

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
