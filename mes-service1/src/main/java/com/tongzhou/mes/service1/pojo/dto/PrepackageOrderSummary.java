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

package com.tongzhou.mes.service1.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "预包装订单信息")
public class PrepackageOrderSummary {

    @Schema(description = "预包装订单ID", example = "1000")
    private Long id;

    @Schema(description = "工单ID", example = "100")
    private Long workOrderId;

    @Schema(description = "批次ID")
    private Long batchId;

    @Schema(description = "批次号（冗余）")
    private String batchNum;

    @Schema(description = "工单号（冗余）")
    private String workId;

    @Schema(description = "订单号", example = "ORDER-001")
    private String orderNum;

    @Schema(description = "发货人")
    private String consignor;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "工单号（业务字段）")
    private String workNum;

    @Schema(description = "收货人")
    private String receiver;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "出货批次号")
    private String shipBatch;

    @Schema(description = "安装地址")
    private String installAddress;

    @Schema(description = "终端客户名")
    private String customer;

    @Schema(description = "收货地区")
    private String receiveRegion;

    @Schema(description = "产品所属空间")
    private String space;

    @Schema(description = "包件类型")
    private String packType;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "预包装总包数")
    private Integer prepackageInfoSize;

    @Schema(description = "总套数")
    private Integer totalSet;

    @Schema(description = "一套内总包数")
    private Integer maxPackageNo;

    @Schema(description = "生产编号")
    private String productionNum;

    @Schema(description = "是否项目")
    private Integer isProject;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "FNUMBER")
    private String fnumber;

    @Schema(description = "DOB")
    private String dob;

    @Schema(description = "详细地址")
    private String detailedAddress;

    @Schema(description = "逻辑删除标识（0-未删除、1-已删除）")
    private Integer isDeleted;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新人")
    private String updatedBy;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
