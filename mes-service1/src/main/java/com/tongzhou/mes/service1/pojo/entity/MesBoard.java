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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 板件表实体类
 * 
 * @author MES Team
 */
@Data
@TableName("mes_part")
public class MesBoard {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 包件ID（外键）
     */
    private Long packageId;

    /**
     * 箱码ID（冗余字段）
     */
    private Long boxId;

    /**
     * 批次号（冗余字段，方便按批次查询）
     */
    private String batchNum;

    /**
     * 工单号（冗余字段）
     */
    private String workId;

    /**
     * 部件条码（唯一）
     */
    private String partCode;

    /**
     * 第几层
     */
    private Integer layer;

    /**
     * 第几片
     */
    private Integer piece;

    /**
     * 板件ID
     */
    private String itemCode;

    /**
     * 板件描述
     */
    private String itemName;

    /**
     * 花色（分拣要的花色）
     */
    private String matName;

    /**
     * 板件长
     */
    private BigDecimal itemLength;

    /**
     * 板件宽
     */
    private BigDecimal itemWidth;

    /**
     * 板件高
     */
    private BigDecimal itemDepth;

    /**
     * X轴坐标
     */
    private BigDecimal xAxis;

    /**
     * Y轴坐标
     */
    private BigDecimal yAxis;

    /**
     * Z轴坐标
     */
    private BigDecimal zAxis;

    /**
     * 分拣出板顺序
     */
    private Integer sortOrder;

    /**
     * 标准码集合（JSON格式）
     */
    private String standardList;

    /**
     * 逻辑删除标识（0-未删除、1-已删除/关联板件已失效）
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
