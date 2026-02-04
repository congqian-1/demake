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
import com.tongzhou.mes.service1.mapper.MesBoxCodeMapper;
import com.tongzhou.mes.service1.mapper.MesOptimizationFileMapper;
import com.tongzhou.mes.service1.mapper.MesPackageMapper;
import com.tongzhou.mes.service1.mapper.MesPrepackageOrderMapper;
import com.tongzhou.mes.service1.mapper.MesWorkOrderMapper;
import com.tongzhou.mes.service1.pojo.dto.BatchSummary;
import com.tongzhou.mes.service1.pojo.dto.BoxSummary;
import com.tongzhou.mes.service1.pojo.dto.OptimizingFileSummary;
import com.tongzhou.mes.service1.pojo.dto.PackageSummary;
import com.tongzhou.mes.service1.pojo.dto.PartDetailResponse;
import com.tongzhou.mes.service1.pojo.dto.PrepackageOrderSummary;
import com.tongzhou.mes.service1.pojo.dto.WorkOrderSummary;
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
    private final MesOptimizationFileMapper optimizationFileMapper;
    private final MesPrepackageOrderMapper prepackageOrderMapper;
    private final MesBoxCodeMapper boxCodeMapper;
    private final MesPackageMapper packageMapper;
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

        // 2. 组装板件响应
        PartDetailResponse response = toPartDetailResponse(board);

        // 3. 组装上层实体
        MesPackage packageEntity = null;
        if (board.getPackageId() != null) {
            packageEntity = packageMapper.selectById(board.getPackageId());
        }
        if (packageEntity != null) {
            response.setPackageInfo(toPackageSummary(packageEntity));
        }

        MesBoxCode boxEntity = null;
        if (board.getBoxId() != null) {
            boxEntity = boxCodeMapper.selectById(board.getBoxId());
        } else if (packageEntity != null && packageEntity.getBoxId() != null) {
            boxEntity = boxCodeMapper.selectById(packageEntity.getBoxId());
        }
        if (boxEntity != null) {
            response.setBox(toBoxSummary(boxEntity));
        }

        MesWorkOrder workOrder = null;
        if (board.getWorkId() != null && !board.getWorkId().trim().isEmpty()) {
            workOrder = workOrderMapper.selectOne(
                new LambdaQueryWrapper<MesWorkOrder>()
                    .eq(MesWorkOrder::getWorkId, board.getWorkId())
            );
        }
        if (workOrder != null) {
            response.setWorkOrder(toWorkOrderSummary(workOrder));
        }

        MesPrepackageOrder prepackageOrder = null;
        if (workOrder != null && workOrder.getWorkId() != null) {
            prepackageOrder = prepackageOrderMapper.selectByWorkId(workOrder.getWorkId());
        } else if (boxEntity != null && boxEntity.getPrepackageOrderId() != null) {
            prepackageOrder = prepackageOrderMapper.selectById(boxEntity.getPrepackageOrderId());
        }
        if (prepackageOrder != null) {
            response.setPrepackageOrder(toPrepackageOrderSummary(prepackageOrder));
        }

        MesOptimizationFile optimizationFile = null;
        if (workOrder != null && workOrder.getOptimizingFileId() != null) {
            optimizationFile = optimizationFileMapper.selectById(workOrder.getOptimizingFileId());
        }
        if (optimizationFile != null) {
            response.setOptimizingFile(toOptimizingFileSummary(optimizationFile));
        }

        MesBatch batch = null;
        if (workOrder != null && workOrder.getBatchId() != null) {
            batch = batchMapper.selectById(workOrder.getBatchId());
        }
        if (batch == null && board.getBatchNum() != null && !board.getBatchNum().trim().isEmpty()) {
            batch = batchMapper.selectByBatchNum(board.getBatchNum());
        }
        if (batch == null && workOrder != null && workOrder.getBatchNum() != null
            && !workOrder.getBatchNum().trim().isEmpty()) {
            batch = batchMapper.selectByBatchNum(workOrder.getBatchNum());
        }
        if (batch != null) {
            response.setBatch(toBatchSummary(batch));
        }

        log.info("查询板件码 {} 的详细信息成功", partCode);

        return response;
    }

    private PartDetailResponse toPartDetailResponse(MesBoard board) {
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
        response.setRealPackageNo(board.getRealPackageNo());
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
                log.warn("解析板件 {} 的standardList失败: {}", board.getPartCode(), e.getMessage());
                response.setStandardListRaw(board.getStandardList());
            }
        }

        return response;
    }

    private PackageSummary toPackageSummary(MesPackage pkg) {
        PackageSummary summary = new PackageSummary();
        summary.setId(pkg.getId());
        summary.setBoxId(pkg.getBoxId());
        summary.setPackageNo(pkg.getPackageNo());
        summary.setLength(pkg.getLength());
        summary.setWidth(pkg.getWidth());
        summary.setDepth(pkg.getDepth());
        summary.setWeight(pkg.getWeight());
        summary.setBoxType(pkg.getBoxType());
        return summary;
    }

    private BoxSummary toBoxSummary(MesBoxCode box) {
        BoxSummary summary = new BoxSummary();
        summary.setId(box.getId());
        summary.setPrepackageOrderId(box.getPrepackageOrderId());
        summary.setBoxCode(box.getBoxCode());
        summary.setBuilding(box.getBuilding());
        summary.setHouse(box.getHouse());
        summary.setRoom(box.getRoom());
        return summary;
    }

    private PrepackageOrderSummary toPrepackageOrderSummary(MesPrepackageOrder order) {
        PrepackageOrderSummary summary = new PrepackageOrderSummary();
        summary.setId(order.getId());
        summary.setWorkOrderId(order.getWorkOrderId());
        summary.setOrderNum(order.getOrderNum());
        summary.setConsignor(order.getConsignor());
        summary.setReceiver(order.getReceiver());
        summary.setInstallAddress(order.getInstallAddress());
        return summary;
    }

    private WorkOrderSummary toWorkOrderSummary(MesWorkOrder workOrder) {
        WorkOrderSummary summary = new WorkOrderSummary();
        summary.setId(workOrder.getId());
        summary.setBatchId(workOrder.getBatchId());
        summary.setOptimizingFileId(workOrder.getOptimizingFileId());
        summary.setWorkId(workOrder.getWorkId());
        summary.setRoute(workOrder.getRoute());
        summary.setOrderType(workOrder.getOrderType());
        summary.setPrepackageStatus(workOrder.getPrepackageStatus());
        return summary;
    }

    private OptimizingFileSummary toOptimizingFileSummary(MesOptimizationFile file) {
        OptimizingFileSummary summary = new OptimizingFileSummary();
        summary.setId(file.getId());
        summary.setBatchId(file.getBatchId());
        summary.setOptimizingFileName(file.getOptimizingFileName());
        summary.setStationCode(file.getStationCode());
        summary.setUrgency(file.getUrgency());
        return summary;
    }

    private BatchSummary toBatchSummary(MesBatch batch) {
        BatchSummary summary = new BatchSummary();
        summary.setId(batch.getId());
        summary.setBatchNum(batch.getBatchNum());
        summary.setBatchType(batch.getBatchType());
        summary.setProductTime(batch.getProductTime());
        return summary;
    }
}
