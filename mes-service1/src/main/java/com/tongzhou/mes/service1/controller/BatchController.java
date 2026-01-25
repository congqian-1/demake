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

import com.tongzhou.mes.service1.pojo.dto.BatchPushRequest;
import com.tongzhou.mes.service1.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 批次控制器
 * 
 * @author MES Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/third-party/batch")
@RequiredArgsConstructor
@Tag(name = "批次管理", description = "第三方MES批次数据推送接口")
public class BatchController {

    private final BatchService batchService;

    /**
     * 批次推送接口（供第三方MES系统调用）
     * 
     * @param request 批次推送请求
     * @return 响应结果
     */
    @PostMapping("/push")
    @Operation(summary = "批次推送", description = "第三方MES系统推送批次及工单数据")
    public ResponseEntity<Map<String, Object>> pushBatch(@Validated @RequestBody BatchPushRequest request) {
        try {
            log.info("收到批次推送请求，批次号: {}", request.getBatchNum());
            
            // 保存批次数据
            String batchNo = batchService.saveBatch(request);
            
            log.info("批次推送成功，批次号: {}", batchNo);
            
            // 构造响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批次推送成功");
            response.put("batchNo", batchNo);
            
            // 统计工单总数
            int workOrderCount = 0;
            if (request.getOptimizingFiles() != null) {
                for (BatchPushRequest.OptimizingFileInfo file : request.getOptimizingFiles()) {
                    if (file.getWorkOrders() != null) {
                        workOrderCount += file.getWorkOrders().size();
                    }
                }
            }
            
            response.put("workOrderCount", workOrderCount);
            response.put("fileCount", request.getOptimizingFiles() != null ? request.getOptimizingFiles().size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批次推送失败，批次号: {}, 错误信息: {}", request.getBatchNum(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "批次推送失败: " + e.getMessage());
            errorResponse.put("batchNo", request.getBatchNum());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
