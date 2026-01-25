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

import java.math.BigDecimal;
import java.util.List;

/**
 * 板件码查询包装数据响应DTO
 *
 * 返回订单→箱码→包件→板件的完整结构字段。
 *
 * @author MES Team
 */
@Data
@Schema(description = "板件码查询包装数据响应")
public class PartPackageResponse {

    @Schema(description = "箱码信息")
    private BoxInfo box;

    @Schema(description = "订单信息")
    private OrderInfo order;

    @Schema(description = "当前板件在包装中的位置信息")
    private PositionInfo position;

    /**
     * 箱码信息
     */
    @Data
    @Schema(description = "箱码信息")
    public static class BoxInfo {

        @Schema(description = "箱码ID")
        private Long id;

        @Schema(description = "预包装订单ID")
        private Long prepackageOrderId;

        @Schema(description = "批次号")
        private String batchNum;

        @Schema(description = "工单号")
        private String workId;

        @Schema(description = "箱码")
        private String boxCode;

        @Schema(description = "楼栋")
        private String building;

        @Schema(description = "户型")
        private String house;

        @Schema(description = "房间号")
        private String room;

        @Schema(description = "套数")
        private Integer setno;

        @Schema(description = "颜色")
        private String color;

        @Schema(description = "箱内板件数量")
        private Integer partCount;

        @Schema(description = "箱内所有板件列表（扁平视图）")
        private List<PartInfo> partList;

        @Schema(description = "箱内所有包件列表")
        private List<PackageInfo> packageList;
    }

    /**
     * 包件信息
     */
    @Data
    @Schema(description = "包件信息")
    public static class PackageInfo {

        @Schema(description = "包件ID")
        private Long id;

        @Schema(description = "箱码ID")
        private Long boxId;

        @Schema(description = "批次号")
        private String batchNum;

        @Schema(description = "工单号")
        private String workId;

        @Schema(description = "箱码")
        private String boxCode;

        @Schema(description = "包号")
        private Integer packageNo;

        @Schema(description = "长度")
        private BigDecimal length;

        @Schema(description = "宽度")
        private BigDecimal width;

        @Schema(description = "高度")
        private BigDecimal depth;

        @Schema(description = "重量")
        private BigDecimal weight;

        @Schema(description = "部件数")
        private Integer partCount;

        @Schema(description = "纸箱类型")
        private String boxType;

        @Schema(description = "该包件下的板件列表")
        private List<PartInfo> partList;
    }

    /**
     * 板件信息
     */
    @Data
    @Schema(description = "板件信息")
    public static class PartInfo {

        @Schema(description = "板件ID")
        private Long id;

        @Schema(description = "板件码")
        private String partCode;

        @Schema(description = "第几层")
        private Integer layer;

        @Schema(description = "第几片")
        private Integer piece;

        @Schema(description = "板件ID（业务标识）")
        private String itemCode;

        @Schema(description = "板件描述")
        private String itemName;

        @Schema(description = "花色")
        private String matName;

        @Schema(description = "板件长")
        private BigDecimal itemLength;

        @Schema(description = "板件宽")
        private BigDecimal itemWidth;

        @Schema(description = "板件高")
        private BigDecimal itemDepth;

        @Schema(description = "X轴坐标")
        private BigDecimal xAxis;

        @Schema(description = "Y轴坐标")
        private BigDecimal yAxis;

        @Schema(description = "Z轴坐标")
        private BigDecimal zAxis;

        @Schema(description = "分拣顺序")
        private Integer sortOrder;

        @Schema(description = "标准码集合（JSON字符串）")
        private String standardList;
    }

    /**
     * 订单信息
     */
    @Data
    @Schema(description = "订单信息")
    public static class OrderInfo {

        @Schema(description = "订单ID")
        private Long id;

        @Schema(description = "工单ID")
        private Long workOrderId;

        @Schema(description = "批次ID")
        private Long batchId;

        @Schema(description = "批次号")
        private String batchNum;

        @Schema(description = "工单号")
        private String workId;

        @Schema(description = "订单号")
        private String orderNum;

        @Schema(description = "客户名称（WMS货主）")
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
    }

    /**
     * 板件位置信息
     */
    @Data
    @Schema(description = "板件位置信息")
    public static class PositionInfo {

        @Schema(description = "所在箱码")
        private String boxCode;

        @Schema(description = "所在包件ID")
        private Long packageId;

        @Schema(description = "箱内位置（分拣顺序）")
        private Integer sortOrder;

        @Schema(description = "箱码层级")
        private Integer boxLayer;
    }
}
