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

package com.tongzhou.mes.service1.controller;

import com.tongzhou.mes.service1.exception.DuplicateWorkReportException;
import com.tongzhou.mes.service1.exception.PartNotFoundException;
import com.tongzhou.mes.service1.pojo.dto.WorkReportRequest;
import com.tongzhou.mes.service1.service.WorkReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 板件报工接口
 *
 * @author MES Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/production/work-report")
@Tag(name = "板件报工管理", description = "产线客户端板件报工接口")
public class WorkReportController {

    @Autowired
    private WorkReportService workReportService;

    /**
     * 提交板件报工记录
     *
     * @param request 报工请求数据
     * @return 报工结果
     */
    @PostMapping
    @Operation(summary = "提交板件报工", description = "产线客户端提交板件报工记录，支持幂等性检查")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "Not found",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "500", description = "Server error",
            content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<Map<String, Object>> submitWorkReport(
            @Validated
            @RequestBody(description = "板件报工请求", required = true,
                content = @Content(schema = @Schema(implementation = WorkReportRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody WorkReportRequest request) {
        log.info("收到板件报工请求: {}", request);

        Map<String, Object> response = new HashMap<>();

        try {
            workReportService.saveWorkReport(request);
            
            response.put("success", true);
            response.put("message", "报工成功");
            response.put("partCode", request.getPartCode());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);

        } catch (PartNotFoundException e) {
            log.error("板件不存在: {}", e.getMessage());
            response.put("success", false);
            response.put("error", "板件不存在");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (DuplicateWorkReportException e) {
            log.warn("重复报工: {}", e.getMessage());
            response.put("success", false);
            response.put("error", "重复报工");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("报工处理失败", e);
            response.put("success", false);
            response.put("error", "系统错误");
            response.put("message", "报工处理失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
