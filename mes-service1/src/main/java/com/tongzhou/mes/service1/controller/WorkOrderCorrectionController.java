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

import com.tongzhou.mes.service1.service.PrePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * 工单数据修正接口
 *
 * @author MES Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/work-order")
@RequiredArgsConstructor
@Tag(name = "工单数据修正管理", description = "管理员工单数据修正接口")
public class WorkOrderCorrectionController {

    private final PrePackageService prePackageService;

    /**
     * 重置工单为未拉取状态，由定时任务后续执行拉取。
     */
    @PostMapping("/{workId}/repull")
    @Operation(summary = "重置工单重拉状态", description = "管理员将工单重置为未拉取状态，不立即调用第三方接口")
    public ResponseEntity<Map<String, Object>> repullWorkOrder(
            @Parameter(description = "工单号", required = true, example = "WO-001")
            @PathVariable String workId,
            @RequestBody RepullRequest request) {
        
        log.info("收到工单数据修正请求，工单号: {}, 操作人: {}, 原因: {}", 
            workId, request.getOperator(), request.getReason());

        Map<String, Object> response = new HashMap<>();

        try {
            prePackageService.repullWorkOrder(workId, request.getOperator(), request.getReason());
            
            response.put("success", true);
            response.put("message", "工单已重置为未拉取");
            response.put("workId", workId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("工单数据修正失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "修正失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 重置批次下全部工单为未拉取状态。
     */
    @PostMapping("/batch/{batchNum}/repull")
    @Operation(summary = "重置批次全部工单重拉状态", description = "管理员将指定批次下全部工单重置为未拉取状态，不立即调用第三方接口")
    public ResponseEntity<Map<String, Object>> repullBatchWorkOrders(
            @Parameter(description = "批次号", required = true, example = "BATCH-001")
            @PathVariable String batchNum,
            @RequestBody RepullRequest request) {

        log.info("收到批次重拉状态重置请求，批次号: {}, 操作人: {}, 原因: {}",
            batchNum, request.getOperator(), request.getReason());

        Map<String, Object> response = new HashMap<>();
        try {
            int resetCount = prePackageService.repullBatchWorkOrders(batchNum, request.getOperator(), request.getReason());
            response.put("success", true);
            response.put("message", "批次工单已重置为未拉取");
            response.put("batchNum", batchNum);
            response.put("resetCount", resetCount);
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批次工单重置失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "批次重置失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 重新拉取请求DTO
     */
    public static class RepullRequest {
        @NotBlank(message = "操作人不能为空")
        private String operator;
        
        @NotBlank(message = "修正原因不能为空")
        private String reason;

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
