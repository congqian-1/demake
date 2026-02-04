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

@Data
@Schema(description = "预包装订单信息")
public class PrepackageOrderSummary {

    @Schema(description = "预包装订单ID", example = "1000")
    private Long id;

    @Schema(description = "工单ID", example = "100")
    private Long workOrderId;

    @Schema(description = "订单号", example = "ORDER-001")
    private String orderNum;

    @Schema(description = "发货人")
    private String consignor;

    @Schema(description = "收货人")
    private String receiver;

    @Schema(description = "安装地址")
    private String installAddress;
}
