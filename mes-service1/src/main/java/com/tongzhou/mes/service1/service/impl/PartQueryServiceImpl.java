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
import com.tongzhou.mes.service1.mapper.*;
import com.tongzhou.mes.service1.pojo.dto.PartDetailResponse;
import com.tongzhou.mes.service1.pojo.dto.PartPackageResponse;
import com.tongzhou.mes.service1.pojo.dto.PartWorkOrderBatchResponse;
import com.tongzhou.mes.service1.pojo.entity.*;
import com.tongzhou.mes.service1.service.PartQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final MesOptimizationFileMapper optimizationFileMapper;
    private final MesBatchMapper batchMapper;
    private final MesPackageMapper packageMapper;
    private final MesBoxCodeMapper boxCodeMapper;
    private final MesPrepackageOrderMapper prepackageOrderMapper;
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
    public PartWorkOrderBatchResponse queryWorkOrderAndBatch(String partCode) {
        log.info("开始查询板件码 {} 的工单与批次信息", partCode);

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

        // 2. 查询工单
        MesWorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<MesWorkOrder>()
                .eq(MesWorkOrder::getWorkId, board.getWorkId())
        );
        if (workOrder == null) {
            log.error("板件码 {} 关联的工单不存在，workId: {}", partCode, board.getWorkId());
            throw new RuntimeException("板件关联的工单不存在");
        }

        // 3. 检查工单状态
        if ("UPDATING".equals(workOrder.getPrepackageStatus())) {
            log.warn("工单 {} 数据正在更新中，拒绝查询", workOrder.getWorkId());
            throw new WorkOrderUpdatingException(workOrder.getWorkId());
        }

        // 4. 查询优化文件
        MesOptimizationFile optimizationFile = optimizationFileMapper.selectById(workOrder.getOptimizingFileId());
        if (optimizationFile == null) {
            log.error("工单 {} 关联的优化文件不存在，optimizingFileId: {}", 
                    workOrder.getWorkId(), workOrder.getOptimizingFileId());
            throw new RuntimeException("工单关联的优化文件不存在");
        }

        // 5. 查询批次
        MesBatch batch = batchMapper.selectById(workOrder.getBatchId());
        if (batch == null) {
            log.error("工单 {} 关联的批次不存在，batchId: {}", workOrder.getWorkId(), workOrder.getBatchId());
            throw new RuntimeException("工单关联的批次不存在");
        }

        // 6. 组装响应
        PartWorkOrderBatchResponse response = new PartWorkOrderBatchResponse();

        // 工单信息
        PartWorkOrderBatchResponse.WorkOrderInfo workOrderInfo = new PartWorkOrderBatchResponse.WorkOrderInfo();
        BeanUtils.copyProperties(workOrder, workOrderInfo);
        response.setWorkOrder(workOrderInfo);

        // 优化文件信息
        PartWorkOrderBatchResponse.OptimizingFileInfo optimizingFileInfo = new PartWorkOrderBatchResponse.OptimizingFileInfo();
        BeanUtils.copyProperties(optimizationFile, optimizingFileInfo);
        response.setOptimizingFile(optimizingFileInfo);

        // 批次信息
        PartWorkOrderBatchResponse.BatchInfo batchInfo = new PartWorkOrderBatchResponse.BatchInfo();
        BeanUtils.copyProperties(batch, batchInfo);
        response.setBatch(batchInfo);

        log.info("查询板件码 {} 的工单与批次信息成功，工单号: {}, 批次号: {}", 
                partCode, workOrder.getWorkId(), batch.getBatchNum());

        return response;
    }

    @Override
    public PartPackageResponse queryPackage(String partCode) {
        log.info("开始查询板件码 {} 的包装数据", partCode);

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

        // 2. 检查工单状态
        checkWorkOrderStatus(board);

        // 3. 查询包件
        MesPackage mesPackage = packageMapper.selectById(board.getPackageId());
        if (mesPackage == null) {
            log.error("板件码 {} 关联的包件不存在，packageId: {}", partCode, board.getPackageId());
            throw new RuntimeException("板件关联的包件不存在");
        }

        // 4. 查询箱码
        MesBoxCode boxCode = boxCodeMapper.selectById(mesPackage.getBoxId());
        if (boxCode == null) {
            log.error("包件 {} 关联的箱码不存在，boxId: {}", mesPackage.getId(), mesPackage.getBoxId());
            throw new RuntimeException("包件关联的箱码不存在");
        }

        // 5. 查询订单
        MesPrepackageOrder order = prepackageOrderMapper.selectById(boxCode.getPrepackageOrderId());
        if (order == null) {
            log.error("箱码 {} 关联的订单不存在，prepackageOrderId: {}", boxCode.getBoxCode(), boxCode.getPrepackageOrderId());
            throw new RuntimeException("箱码关联的订单不存在");
        }

        // 6. 查询箱内所有包件和板件
        List<MesPackage> packagesInBox = packageMapper.selectList(
                new LambdaQueryWrapper<MesPackage>()
                        .eq(MesPackage::getBoxId, boxCode.getId())
        );
        
        List<Long> packageIds = new ArrayList<>();
        for (MesPackage pkg : packagesInBox) {
            packageIds.add(pkg.getId());
        }
        
        List<MesBoard> partsInBox = new ArrayList<>();
        if (!packageIds.isEmpty()) {
            partsInBox = boardMapper.selectList(
                    new LambdaQueryWrapper<MesBoard>()
                            .in(MesBoard::getPackageId, packageIds)
                            .eq(MesBoard::getIsDeleted, 0)
                            .orderByAsc(MesBoard::getSortOrder)
            );
        }

        // 7. 组装响应
        PartPackageResponse response = new PartPackageResponse();

        // 7.1 组装板件列表（扁平 + 按包件分组）
        List<PartPackageResponse.PartInfo> flatPartList = new ArrayList<>();
        Map<Long, List<PartPackageResponse.PartInfo>> partsByPackageId = new HashMap<>();
        for (MesBoard part : partsInBox) {
            PartPackageResponse.PartInfo partInfo = new PartPackageResponse.PartInfo();
            partInfo.setId(part.getId());
            partInfo.setPartCode(part.getPartCode());
            partInfo.setLayer(part.getLayer());
            partInfo.setPiece(part.getPiece());
            partInfo.setItemCode(part.getItemCode());
            partInfo.setItemName(part.getItemName());
            partInfo.setMatName(part.getMatName());
            partInfo.setItemLength(part.getItemLength());
            partInfo.setItemWidth(part.getItemWidth());
            partInfo.setItemDepth(part.getItemDepth());
            partInfo.setXAxis(part.getXAxis());
            partInfo.setYAxis(part.getYAxis());
            partInfo.setZAxis(part.getZAxis());
            partInfo.setSortOrder(part.getSortOrder());
            partInfo.setStandardList(part.getStandardList());

            flatPartList.add(partInfo);
            partsByPackageId.computeIfAbsent(part.getPackageId(), key -> new ArrayList<>()).add(partInfo);
        }

        // 7.2 组装包件列表
        List<PartPackageResponse.PackageInfo> packageInfoList = new ArrayList<>();
        for (MesPackage pkg : packagesInBox) {
            PartPackageResponse.PackageInfo packageInfo = new PartPackageResponse.PackageInfo();
            packageInfo.setId(pkg.getId());
            packageInfo.setBoxId(pkg.getBoxId());
            packageInfo.setBatchNum(pkg.getBatchNum());
            packageInfo.setWorkId(pkg.getWorkId());
            packageInfo.setBoxCode(pkg.getBoxCode());
            packageInfo.setPackageNo(pkg.getPackageNo());
            packageInfo.setLength(pkg.getLength());
            packageInfo.setWidth(pkg.getWidth());
            packageInfo.setDepth(pkg.getDepth());
            packageInfo.setWeight(pkg.getWeight());
            packageInfo.setPartCount(pkg.getPartCount());
            packageInfo.setBoxType(pkg.getBoxType());
            packageInfo.setPartList(partsByPackageId.getOrDefault(pkg.getId(), new ArrayList<>()));
            packageInfoList.add(packageInfo);
        }

        // 7.3 箱码信息
        PartPackageResponse.BoxInfo boxInfo = new PartPackageResponse.BoxInfo();
        boxInfo.setId(boxCode.getId());
        boxInfo.setPrepackageOrderId(boxCode.getPrepackageOrderId());
        boxInfo.setBatchNum(boxCode.getBatchNum());
        boxInfo.setWorkId(boxCode.getWorkId());
        boxInfo.setBoxCode(boxCode.getBoxCode());
        boxInfo.setBuilding(boxCode.getBuilding());
        boxInfo.setHouse(boxCode.getHouse());
        boxInfo.setRoom(boxCode.getRoom());
        boxInfo.setSetno(boxCode.getSetno());
        boxInfo.setColor(boxCode.getColor());
        boxInfo.setPartCount(flatPartList.size());
        boxInfo.setPartList(flatPartList);
        boxInfo.setPackageList(packageInfoList);
        response.setBox(boxInfo);

        // 7.4 订单信息（全量字段）
        PartPackageResponse.OrderInfo orderInfo = new PartPackageResponse.OrderInfo();
        orderInfo.setId(order.getId());
        orderInfo.setWorkOrderId(order.getWorkOrderId());
        orderInfo.setBatchId(order.getBatchId());
        orderInfo.setBatchNum(order.getBatchNum());
        orderInfo.setWorkId(order.getWorkId());
        orderInfo.setOrderNum(order.getOrderNum());
        orderInfo.setConsignor(order.getConsignor());
        orderInfo.setContractNo(order.getContractNo());
        orderInfo.setWorkNum(order.getWorkNum());
        orderInfo.setReceiver(order.getReceiver());
        orderInfo.setPhone(order.getPhone());
        orderInfo.setShipBatch(order.getShipBatch());
        orderInfo.setInstallAddress(order.getInstallAddress());
        orderInfo.setCustomer(order.getCustomer());
        orderInfo.setReceiveRegion(order.getReceiveRegion());
        orderInfo.setSpace(order.getSpace());
        orderInfo.setPackType(order.getPackType());
        orderInfo.setProductType(order.getProductType());
        orderInfo.setPrepackageInfoSize(order.getPrepackageInfoSize());
        orderInfo.setTotalSet(order.getTotalSet());
        orderInfo.setMaxPackageNo(order.getMaxPackageNo());
        orderInfo.setProductionNum(order.getProductionNum());
        response.setOrder(orderInfo);

        // 板件位置信息
        PartPackageResponse.PositionInfo positionInfo = new PartPackageResponse.PositionInfo();
        positionInfo.setBoxCode(boxCode.getBoxCode());
        positionInfo.setPackageId(board.getPackageId());
        positionInfo.setSortOrder(board.getSortOrder());
        positionInfo.setBoxLayer(board.getLayer()); // 使用板件自身的layer字段
        response.setPosition(positionInfo);

        log.info("查询板件码 {} 的包装数据成功，箱码: {}, 箱内板件数: {}", 
                partCode, boxCode.getBoxCode(), flatPartList.size());

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
