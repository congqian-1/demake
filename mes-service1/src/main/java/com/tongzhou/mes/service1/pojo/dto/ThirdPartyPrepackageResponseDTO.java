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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        private Integer isProject;
        private String space;
        private String customerName;
        private String receiveRegion;
        @JsonProperty("FNUMBER")
        private String fnumber;
        private Integer maxPackageNo;
        private String workNum;
        private String phone;
        private String dob;
        private String detailedAddress;
        private String consignor;
        private String productType;
        @JsonAlias({"Type"})
        private String type;
        @JsonProperty("FDD8")
        @JsonAlias({"fdd8"})
        private String fdd8;
        private String customer;
        private List<PrePackageInfoItem> prePackageInfo;
    }

    @Data
    public static class PrePackageInfoItem {
        private String boxCode;
        private Integer setno;
        private String building;
        private String house;
        private String room;
        private String color;
        private String unit;
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
        private String building;
        private String house;
        private String room;
        private String color;
        private String unit;
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
        private String itemLength;
        private String itemWidth;
        private String itemDepth;
        @JsonProperty("xAxis")
        private String xAxis;
        @JsonProperty("yAxis")
        private String yAxis;
        @JsonProperty("zAxis")
        private String zAxis;
        private String standardCode;
        @JsonAlias({"Rotate"})
        private String rotate;
        @JsonAlias({"ProcessCode", "process_code"})
        private String processCode;
        @JsonAlias({"Workmanship"})
        private String workmanship;
        @JsonAlias({"OrderNumber"})
        private String orderNumber;
        @JsonAlias({"SealingFlatNoodles"})
        private String sealingFlatNoodles;
        @JsonAlias({"Texture"})
        private String texture;
        @JsonAlias({"ContainerNumber"})
        private String containerNumber;
        @JsonAlias({"SetNumber"})
        private String setNumber;
        @JsonAlias({"Groove"})
        private String groove;
        private Integer condition;

        public void setWorkmanship(Object workmanship) {
            this.workmanship = asString(workmanship);
        }

        public void setOrderNumber(Object orderNumber) {
            this.orderNumber = asString(orderNumber);
        }

        public void setSealingFlatNoodles(Object sealingFlatNoodles) {
            this.sealingFlatNoodles = asString(sealingFlatNoodles);
        }

        public void setTexture(Object texture) {
            this.texture = asString(texture);
        }

        public void setContainerNumber(Object containerNumber) {
            this.containerNumber = asString(containerNumber);
        }

        public void setSetNumber(Object setNumber) {
            this.setNumber = asString(setNumber);
        }

        public void setGroove(Object groove) {
            this.groove = asString(groove);
        }

        private static String asString(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    }
}
