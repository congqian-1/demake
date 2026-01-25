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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预包装数据DTO（从第三方MES拉取的预包装数据）
 * 
 * @author MES Team
 */
@Data
@Schema(description = "预包装数据响应")
public class PrepackageDataDTO {

    @JsonProperty("PrePackageInfo")
    @Schema(description = "预包装信息")
    private PrePackageInfo prePackageInfo;

    /**
     * 预包装信息
     */
    @Data
    @Schema(description = "预包装订单信息")
    public static class PrePackageInfo {

        @JsonProperty("OrderNum")
        private String orderNum; // 订单号

        @JsonProperty("Consignor")
        private String consignor; // 客户名称（WMS货主）

        @JsonProperty("ContractNo")
        private String contractNo; // 合同编号

        @JsonProperty("WorkNum")
        private String workNum; // 工单号

        @JsonProperty("Receiver")
        private String receiver; // 收货人

        @JsonProperty("Phone")
        private String phone; // 联系电话

        @JsonProperty("ShipBatch")
        private String shipBatch; // 出货批次号

        @JsonProperty("InstallAddress")
        private String installAddress; // 安装地址

        @JsonProperty("Customer")
        private String customer; // 终端客户名

        @JsonProperty("ReceiveRegion")
        private String receiveRegion; // 收货地区

        @JsonProperty("Space")
        private String space; // 产品所属空间

        @JsonProperty("PackType")
        private String packType; // 包件类型

        @JsonProperty("ProductType")
        private String productType; // 产品类型

        @JsonProperty("PrepackageInfoSize")
        private Integer prepackageInfoSize; // 预包装总包数

        @JsonProperty("TotalSet")
        private Integer totalSet; // 总套数

        @JsonProperty("MaxPackageNo")
        private Integer maxPackageNo; // 一套内的总包数

        @JsonProperty("ProductionNum")
        private String productionNum; // 生产编号

        @JsonProperty("BoxInfoDetails")
        private List<BoxInfoDetail> boxInfoDetails; // 箱码详情列表
    }

    /**
     * 箱码详情
     */
    @Data
    @Schema(description = "箱码详情")
    public static class BoxInfoDetail {

        @JsonProperty("BoxCode")
        private String boxCode; // 箱码

        @JsonProperty("Building")
        private String building; // 楼栋

        @JsonProperty("House")
        private String house; // 户型

        @JsonProperty("Room")
        private String room; // 房间号

        @JsonProperty("Setno")
        private Integer setno; // 第几套

        @JsonProperty("Color")
        private String color; // 颜色

        @JsonProperty("PackageInfos")
        private List<PackageInfo> packageInfos; // 包件信息列表
    }

    /**
     * 包件信息
     */
    @Data
    @Schema(description = "包件信息")
    public static class PackageInfo {

        @JsonProperty("PackageNo")
        private Integer packageNo; // 第几包

        @JsonProperty("Length")
        private BigDecimal length; // 长度（单位：mm）

        @JsonProperty("Width")
        private BigDecimal width; // 宽度（单位：mm）

        @JsonProperty("Depth")
        private BigDecimal depth; // 高度（单位：mm）

        @JsonProperty("Weight")
        private BigDecimal weight; // 重量（单位：kg）

        @JsonProperty("PartCount")
        private Integer partCount; // 部件数

        @JsonProperty("BoxType")
        private String boxType; // 纸箱类型（如"地盖"）

        @JsonProperty("PartInfos")
        private List<PartInfo> partInfos; // 板件信息列表
    }

    /**
     * 板件信息
     */
    @Data
    @Schema(description = "板件信息")
    public static class PartInfo {

        @JsonProperty("PartCode")
        private String partCode; // 部件条码

        @JsonProperty("Layer")
        private Integer layer; // 第几层

        @JsonProperty("Piece")
        private Integer piece; // 第几片

        @JsonProperty("ItemCode")
        private String itemCode; // 板件ID

        @JsonProperty("ItemName")
        private String itemName; // 板件描述

        @JsonProperty("MatName")
        private String matName; // 花色

        @JsonProperty("ItemLength")
        private BigDecimal itemLength; // 板件长

        @JsonProperty("ItemWidth")
        private BigDecimal itemWidth; // 板件宽

        @JsonProperty("ItemDepth")
        private BigDecimal itemDepth; // 板件高

        @JsonProperty("XAxis")
        private BigDecimal xAxis; // X轴坐标

        @JsonProperty("YAxis")
        private BigDecimal yAxis; // Y轴坐标

        @JsonProperty("ZAxis")
        private BigDecimal zAxis; // Z轴坐标

        @JsonProperty("SortOrder")
        private Integer sortOrder; // 分拣出板顺序

        @JsonProperty("StandardList")
        private String standardListJson; // 标准码集合（JSON格式）
    }
}
