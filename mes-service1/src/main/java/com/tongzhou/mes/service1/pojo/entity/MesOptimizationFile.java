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
 * 优化文件表实体类
 * 
 * @author MES Team
 */
@Data
@TableName("mes_optimizing_file")
public class MesOptimizationFile {

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
     * 批次号（冗余字段）
     */
    private String batchNum;

    /**
     * 优化文件名称
     */
    private String optimizingFileName;

    /**
     * 工位编码（C1A001/C1A002/CMA001/CMA002/YMA001/YMA002）
     */
    private String stationCode;

    /**
     * 是否加急（0=不加急/1=加急）
     */
    private Integer urgency;

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
