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
import lombok.Data;

import java.util.List;

/**
 * MES batchQuery 接口响应 DTO
 *
 * @author MES Team
 */
@Data
public class BatchQueryResponseDTO {

    private Integer code;

    private String msg;

    private List<BatchQueryItem> data;

    @Data
    public static class BatchQueryItem {

        /** 工单号 */
        @JsonProperty("FGDH")
        private String fgdh;

        /** 批次号 */
        @JsonProperty("FPJH")
        private String fpjh;

        /** 顺序/序号 */
        @JsonProperty("FSX")
        private Integer fsx;

        /** 工序名称 */
        @JsonProperty("FGYNM")
        private String fgnym;

        /** 记录序号 */
        @JsonProperty("RN")
        private Integer rn;

        /** 板件码 */
        @JsonProperty("FTM")
        private String ftm;
    }
}
