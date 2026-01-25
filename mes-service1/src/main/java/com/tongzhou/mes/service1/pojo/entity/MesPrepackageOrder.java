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
 * 预包装订单表实体类
 * 
 * @author MES Team
 */
@Data
@TableName("mes_prepackage_order")
public class MesPrepackageOrder {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工单ID（外键）
     */
    private Long workOrderId;

    /**
     * 批次ID（冗余字段，方便按批次统计）
     */
    private Long batchId;

    /**
     * 批次号（冗余字段，方便查询）
     */
    private String batchNum;

    /**
     * 工单号（冗余字段）
     */
    private String workId;

    /**
     * 订单号
     */
    private String orderNum;

    /**
     * 客户名称（WMS货主）
     */
    private String consignor;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 工单号
     */
    private String workNum;

    /**
     * 收货人
     */
    private String receiver;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 出货批次号
     */
    private String shipBatch;

    /**
     * 安装地址
     */
    private String installAddress;

    /**
     * 终端客户名
     */
    private String customer;

    /**
     * 收货地区
     */
    private String receiveRegion;

    /**
     * 产品所属空间
     */
    private String space;

    /**
     * 包件类型
     */
    private String packType;

    /**
     * 产品类型
     */
    private String productType;

    /**
     * 预包装总包数
     */
    private Integer prepackageInfoSize;

    /**
     * 总套数
     */
    private Integer totalSet;

    /**
     * 一套内的总包数
     */
    private Integer maxPackageNo;

    /**
     * 生产编号
     */
    private String productionNum;

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
