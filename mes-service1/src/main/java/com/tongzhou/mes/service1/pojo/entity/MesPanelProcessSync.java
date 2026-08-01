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

package com.tongzhou.mes.service1.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 看板工序同步记录表实体类。
 * 按 (batch_num, work_id) 唯一，每个工单一条记录。
 *
 * @author MES Team
 */
@Data
@TableName("mes_panel_process_sync")
public class MesPanelProcessSync {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号 */
    private String batchNum;

    /** 工单号 */
    private String workId;

    /** 同步结果：SUCCESS / FAILED */
    private String syncResult;

    /** 失败原因详情（TEXT），成功时为空 */
    private String errorDetail;

    /** 同步完成时间 */
    private LocalDateTime syncedAt;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
