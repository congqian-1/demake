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
     * 重新拉取工单预包装数据（数据修正）
     *
     * @param workId 工单号
     * @param request 修正请求
     * @return 修正结果
     */
    @PostMapping("/{workId}/repull")
    @Operation(summary = "重新拉取工单数据", description = "管理员重新拉取工单预包装数据（数据修正），保留报工记录，软删除板件，物理删除包件/箱码/订单")
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
            response.put("message", "工单数据修正成功");
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
