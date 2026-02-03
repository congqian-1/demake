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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 板件报工请求DTO
 *
 * @author MES Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkReportRequest {

    /**
     * 板件码（唯一标识）
     */
    @NotBlank(message = "板件码不能为空")
    private String partCode;

    /**
     * 板件状态（如：待加工、加工中、已完成等）
     */
    @NotBlank(message = "板件状态不能为空")
    private String partStatus;

    /**
     * 工位编码
     */
    @NotBlank(message = "工位编码不能为空")
    private String stationCode;

    /**
     * 工位名称
     */
    private String stationName;

    /**
     * 操作工ID（可选）
     */
    private String operatorId;

    /**
     * 操作工姓名（可选）
     */
    private String operatorName;

    /**
     * 是否完成（0=未完成/1=已完成）
     */
    @NotNull(message = "完成状态不能为空")
    private Integer isCompleted;

    /**
     * 真实打包包号（可选）
     */
    private String realPackageNo;
}
