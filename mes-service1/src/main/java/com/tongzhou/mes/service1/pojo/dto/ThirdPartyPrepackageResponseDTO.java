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

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 第三方MES预包装新格式响应
 */
@Data
public class ThirdPartyPrepackageResponseDTO {
    private Integer code;
    private String msg;
    private List<DataItem> data;

    @Data
    public static class DataItem {
        private Integer prePackageInfoSize;
        private String receiver;
        private String contractNo;
        private String packType;
        private String orderNum;
        private Integer totalSet;
        private String shipBatch;
        private String installAddress;
        private String space;
        private String customerName;
        private String receiveRegion;
        private Integer maxPackageNo;
        private String workNum;
        private String phone;
        private String consignor;
        private String productType;
        private String customer;
        private List<PrePackageInfoItem> prePackageInfo;
    }

    @Data
    public static class PrePackageInfoItem {
        private Integer setno;
        private List<BoxInfoItem> boxInfoList;
    }

    @Data
    public static class BoxInfoItem {
        private String boxCode;
        private Integer setno;
        private Integer partCount;
        private Integer packageNo;
        private BigDecimal length;
        private BigDecimal width;
        private BigDecimal depth;
        private BigDecimal weight;
        private String boxType;
        private String boxType2;
        private List<PartInfoItem> partInfoList;
    }

    @Data
    public static class PartInfoItem {
        private String partCode;
        private String itemCode;
        private String itemName;
        private String matName;
        private Integer layer;
        private Integer piece;
        private Integer sortOrder;
        private BigDecimal itemLength;
        private BigDecimal itemWidth;
        private BigDecimal itemDepth;
        private BigDecimal xAxis;
        private BigDecimal yAxis;
        private BigDecimal zAxis;
        private Integer condition;
    }
}
