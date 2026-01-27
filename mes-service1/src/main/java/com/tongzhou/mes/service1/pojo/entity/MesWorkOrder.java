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
 * 工单表实体类
 * 
 * @author MES Team
 */
@Data
@TableName("mes_work_order")
public class MesWorkOrder {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次ID（外键）
     */
    private Long batchId;

    /**
     * 优化文件ID（外键）
     */
    private Long optimizingFileId;

    /**
     * 批次号（冗余字段，方便查询）
     */
    private String batchNum;

    /**
     * 工单号（唯一）
     */
    private String workId;

    /**
     * 线路
     */
    private String route;

    /**
     * 线路ID
     */
    private String routeId;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 交付日期
     */
    private LocalDateTime deliveryTime;

    /**
     * 开料/排样时间
     */
    private LocalDateTime nestingTime;

    /**
     * 线路/区域信息
     */
    private String ymba014;

    /**
     * 工位/区域信息
     */
    private String ymba015;

    /**
     * 属性标识
     */
    private String ymba016;

    /**
     * 部件字段
     */
    private String part0;

    /**
     * 条件字段
     */
    private String condition0;

    /**
     * 部件时间字段
     */
    private LocalDateTime partTime0;

    /**
     * 组/套标记
     */
    private Integer zuz;

    /**
     * 预包装数据拉取状态（NOT_PULLED=未拉取/PULLING=拉取中/PULLED=已拉取/FAILED=拉取失败/NO_DATA=无预包装数据/UPDATING=更新中）
     */
    private String prepackageStatus;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最后拉取时间
     */
    private LocalDateTime lastPullTime;

    /**
     * 错误信息（拉取失败时）
     */
    private String errorMessage;

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
