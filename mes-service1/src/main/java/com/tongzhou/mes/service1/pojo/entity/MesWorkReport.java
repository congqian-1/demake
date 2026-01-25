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

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报工记录表实体类
 * 
 * @author MES Team
 */
@Data
@TableName("mes_work_report")
public class MesWorkReport {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 板件ID（外键）
     */
    @TableField(exist = false)
    private Long partId;

    /**
     * 批次号（冗余字段，方便按批次查询）
     */
    @TableField(exist = false)
    private String batchNum;

    /**
     * 工单号（冗余字段）
     */
    private String workId;

    /**
     * 板件码
     */
    private String partCode;

    /**
     * 板件状态值
     */
    private String partStatus;

    /**
     * 工位编码
     */
    private String stationCode;

    /**
     * 工位名称
     */
    private String stationName;

    /**
     * 操作工ID
     */
    @TableField(exist = false)
    private String operatorId;

    /**
     * 操作工姓名
     */
    @TableField(exist = false)
    private String operatorName;

    /**
     * 报工时间
     */
    private LocalDateTime reportTime;

    /**
     * 是否完成（0=未完成/1=已完成）
     */
    @TableField(exist = false)
    private Integer isCompleted;

    /**
     * 逻辑删除标识（0-未删除、1-已删除）
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
