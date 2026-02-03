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

package com.tongzhou.mes.service1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongzhou.mes.service1.exception.PartNotFoundException;
import com.tongzhou.mes.service1.exception.WorkOrderUpdatingException;
import com.tongzhou.mes.service1.mapper.MesBatchMapper;
import com.tongzhou.mes.service1.mapper.MesBoardMapper;
import com.tongzhou.mes.service1.mapper.MesWorkOrderMapper;
import com.tongzhou.mes.service1.pojo.dto.PartDetailResponse;
import com.tongzhou.mes.service1.pojo.dto.hierarchy.ResultBatchHierarchy;
import com.tongzhou.mes.service1.pojo.dto.hierarchy.ResultPrepackageHierarchy;
import com.tongzhou.mes.service1.pojo.entity.*;
import com.tongzhou.mes.service1.service.BatchPackagingQueryService;
import com.tongzhou.mes.service1.service.PartQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 板件查询服务实现类
 * 
 * @author MES Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartQueryServiceImpl implements PartQueryService {

    private final MesBoardMapper boardMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final MesBatchMapper batchMapper;
    private final BatchPackagingQueryService batchPackagingQueryService;
    private final ObjectMapper objectMapper;

    /**
     * 检查工单状态
     */
    private void checkWorkOrderStatus(MesBoard board) {
        MesWorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<MesWorkOrder>()
                .eq(MesWorkOrder::getWorkId, board.getWorkId())
        );
        if (workOrder != null && "UPDATING".equals(workOrder.getPrepackageStatus())) {
            log.warn("工单 {} 数据正在更新中，拒绝查询", workOrder.getWorkId());
            throw new WorkOrderUpdatingException(workOrder.getWorkId());
        }
    }

    @Override
    public ResultBatchHierarchy queryWorkOrderAndBatch(String partCode) {
        log.info("开始查询板件码 {} 的批次层级信息", partCode);

        MesBoard board = boardMapper.selectOne(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getPartCode, partCode)
                .eq(MesBoard::getIsDeleted, 0)
        );

        if (board == null) {
            log.warn("板件码 {} 不存在", partCode);
            throw new PartNotFoundException(partCode);
        }

        MesWorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<MesWorkOrder>()
                .eq(MesWorkOrder::getWorkId, board.getWorkId())
        );
        if (workOrder == null) {
            log.error("板件码 {} 关联的工单不存在，workId: {}", partCode, board.getWorkId());
            throw new RuntimeException("板件关联的工单不存在");
        }

        if ("UPDATING".equals(workOrder.getPrepackageStatus())) {
            log.warn("工单 {} 数据正在更新中，拒绝查询", workOrder.getWorkId());
            throw new WorkOrderUpdatingException(workOrder.getWorkId());
        }

        String batchNum = board.getBatchNum();
        if (batchNum == null || batchNum.trim().isEmpty()) {
            MesBatch batch = batchMapper.selectById(workOrder.getBatchId());
            if (batch != null) {
                batchNum = batch.getBatchNum();
            }
        }

        ResultBatchHierarchy response = new ResultBatchHierarchy();
        response.setCode("0");
        response.setMessage("OK");
        response.setData(batchPackagingQueryService.getBatchHierarchy(batchNum));

        log.info("查询板件码 {} 的批次层级信息成功，批次号: {}", partCode, batchNum);
        return response;
    }

    @Override
    public ResultPrepackageHierarchy queryPackage(String partCode) {
        log.info("开始查询板件码 {} 的包装层级信息", partCode);

        MesBoard board = boardMapper.selectOne(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getPartCode, partCode)
                .eq(MesBoard::getIsDeleted, 0)
        );

        if (board == null) {
            log.warn("板件码 {} 不存在", partCode);
            throw new PartNotFoundException(partCode);
        }

        checkWorkOrderStatus(board);

        ResultPrepackageHierarchy response = new ResultPrepackageHierarchy();
        response.setCode("0");
        response.setMessage("OK");
        response.setData(batchPackagingQueryService.getPrepackageHierarchy(null, board.getWorkId()));

        log.info("查询板件码 {} 的包装层级信息成功，工单号: {}", partCode, board.getWorkId());
        return response;
    }

    @Override
    public PartDetailResponse queryDetail(String partCode) {
        log.info("开始查询板件码 {} 的详细信息", partCode);

        // 1. 查询板件
        MesBoard board = boardMapper.selectOne(
                new LambdaQueryWrapper<MesBoard>()
                        .eq(MesBoard::getPartCode, partCode)
                        .eq(MesBoard::getIsDeleted, 0)
        );

        if (board == null) {
            log.warn("板件码 {} 不存在", partCode);
            throw new PartNotFoundException(partCode);
        }

        // 2. 组装响应
        PartDetailResponse response = new PartDetailResponse();
        response.setId(board.getId());
        response.setPartCode(board.getPartCode());
        response.setBatchNum(board.getBatchNum());
        response.setWorkId(board.getWorkId());
        response.setBoxId(board.getBoxId());
        response.setPackageId(board.getPackageId());
        response.setLayer(board.getLayer());
        response.setPiece(board.getPiece());
        response.setItemCode(board.getItemCode());
        response.setItemName(board.getItemName());
        response.setMatName(board.getMatName());
        response.setItemLength(board.getItemLength());
        response.setItemWidth(board.getItemWidth());
        response.setItemDepth(board.getItemDepth());
        response.setXAxis(board.getXAxis());
        response.setYAxis(board.getYAxis());
        response.setZAxis(board.getZAxis());
        response.setSortOrder(board.getSortOrder());
        response.setIsDeleted(board.getIsDeleted());
        response.setCreatedTime(board.getCreatedTime());
        response.setUpdatedTime(board.getUpdatedTime());

        // 兼容字段
        response.setDescription(board.getItemName());
        response.setColor(board.getMatName());
        
        // 解析standardList JSON
        if (board.getStandardList() != null && !board.getStandardList().isEmpty()) {
            try {
                List<Map<String, Integer>> standardList = objectMapper.readValue(
                    board.getStandardList(), 
                    new TypeReference<List<Map<String, Integer>>>() {}
                );
                response.setStandardList(standardList);
                response.setStandardListRaw(board.getStandardList());
            } catch (Exception e) {
                log.warn("解析板件 {} 的standardList失败: {}", partCode, e.getMessage());
                response.setStandardListRaw(board.getStandardList());
            }
        }

        log.info("查询板件码 {} 的详细信息成功", partCode);

        return response;
    }

}
