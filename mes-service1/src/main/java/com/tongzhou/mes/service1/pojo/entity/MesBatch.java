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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次表实体类
 * 
 * @author MES Team
 */
@Data
@TableName("mes_batch")
public class MesBatch {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次号（唯一）
     */
    private String batchNum;

    /**
     * 批次类型（1=衣柜柜体/2=橱柜柜体/3=衣柜门板/4=橱柜门板/5=合并条码/6=补板）
     */
    private Integer batchType;

    /**
     * 生产日期
     */
    private LocalDate productTime;

    /**
     * 简易批次号
     */
    private String simpleBatchNum;

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
