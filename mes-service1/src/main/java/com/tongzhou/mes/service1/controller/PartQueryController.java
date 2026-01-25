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

import com.tongzhou.mes.service1.exception.PartNotFoundException;
import com.tongzhou.mes.service1.exception.WorkOrderUpdatingException;
import com.tongzhou.mes.service1.pojo.dto.PartDetailResponse;
import com.tongzhou.mes.service1.pojo.dto.PartPackageResponse;
import com.tongzhou.mes.service1.pojo.dto.PartWorkOrderBatchResponse;
import com.tongzhou.mes.service1.service.PartQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 板件查询控制器
 * 
 * @author MES Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/production/part")
@RequiredArgsConstructor
@Tag(name = "板件查询管理", description = "产线客户端板件查询接口")
public class PartQueryController {

    private final PartQueryService partQueryService;

    /**
     * 根据板件码查询工单与批次信息
     * 
     * @param partCode 板件码
     * @return 工单、优化文件、批次信息
     */
    @GetMapping("/{partCode}/work-order-and-batch")
    @Operation(summary = "查询工单与批次信息", description = "根据板件码查询该板件所属工单、优化文件和批次的完整信息")
    public ResponseEntity<?> queryWorkOrderAndBatch(
            @Parameter(description = "板件码", required = true, example = "PART-001")
            @PathVariable String partCode) {
        
        try {
            log.info("收到板件码查询工单与批次信息请求，板件码: {}", partCode);
            
            PartWorkOrderBatchResponse response = partQueryService.queryWorkOrderAndBatch(partCode);
            
            log.info("板件码查询工单与批次信息成功，板件码: {}", partCode);
            return ResponseEntity.ok(response);
            
        } catch (PartNotFoundException e) {
            log.warn("板件码不存在，板件码: {}", partCode);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("partCode", partCode);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (WorkOrderUpdatingException e) {
            log.warn("工单数据更新中，板件码: {}, 工单号: {}", partCode, e.getWorkId());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("partCode", partCode);
            errorResponse.put("workId", e.getWorkId());
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            
        } catch (Exception e) {
            log.error("查询工单与批次信息失败，板件码: {}, 错误信息: {}", partCode, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());
            errorResponse.put("partCode", partCode);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 根据板件码查询包装数据
     * 
     * @param partCode 板件码
     * @return 箱码、订单、板件位置信息
     */
    @GetMapping("/{partCode}/package")
    @Operation(summary = "查询包装数据", description = "根据板件码查询包装结构信息（订单→箱码→包件→板件）")
    public ResponseEntity<?> queryPackage(
            @Parameter(description = "板件码", required = true, example = "PART-001")
            @PathVariable String partCode) {
        
        try {
            log.info("收到板件码查询包装数据请求，板件码: {}", partCode);
            
            PartPackageResponse response = partQueryService.queryPackage(partCode);
            
            log.info("板件码查询包装数据成功，板件码: {}", partCode);
            return ResponseEntity.ok(response);
            
        } catch (PartNotFoundException e) {
            log.warn("板件码不存在，板件码: {}", partCode);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("partCode", partCode);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (WorkOrderUpdatingException e) {
            log.warn("工单数据更新中，板件码: {}, 工单号: {}", partCode, e.getWorkId());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("partCode", partCode);
            errorResponse.put("workId", e.getWorkId());
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            
        } catch (Exception e) {
            log.error("查询包装数据失败，板件码: {}, 错误信息: {}", partCode, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());
            errorResponse.put("partCode", partCode);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 根据板件码查询板件详细信息
     * 
     * @param partCode 板件码
     * @return 板件详细信息
     */
    @GetMapping("/{partCode}/detail")
    @Operation(summary = "查询板件详细信息", description = "根据板件码查询板件自身的全部详细信息")
    public ResponseEntity<?> queryDetail(
            @Parameter(description = "板件码", required = true, example = "PART-001")
            @PathVariable String partCode) {
        
        try {
            log.info("收到板件码查询详细信息请求，板件码: {}", partCode);
            
            PartDetailResponse response = partQueryService.queryDetail(partCode);
            
            log.info("板件码查询详细信息成功，板件码: {}", partCode);
            return ResponseEntity.ok(response);
            
        } catch (PartNotFoundException e) {
            log.warn("板件码不存在，板件码: {}", partCode);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("partCode", partCode);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            log.error("查询板件详细信息失败，板件码: {}, 错误信息: {}", partCode, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());
            errorResponse.put("partCode", partCode);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
