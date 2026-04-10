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

import com.tongzhou.mes.service1.exception.BatchNotFoundException;
import com.tongzhou.mes.service1.exception.PrepackageOrderNotFoundException;
import com.tongzhou.mes.service1.mapper.*;
import com.tongzhou.mes.service1.pojo.dto.hierarchy.*;
import com.tongzhou.mes.service1.pojo.entity.*;
import com.tongzhou.mes.service1.service.BatchPackagingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch and prepackage hierarchy query service implementation.
 */
@Service
@RequiredArgsConstructor
public class BatchPackagingQueryServiceImpl implements BatchPackagingQueryService {

    private final MesBatchMapper batchMapper;
    private final MesOptimizationFileMapper optimizationFileMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final MesPrepackageOrderMapper prepackageOrderMapper;
    private final MesBoxCodeMapper boxCodeMapper;
    private final MesPackageMapper packageMapper;
    private final MesBoardMapper boardMapper;
    private final MesWorkReportMapper workReportMapper;

    @Override
    public BatchHierarchy getBatchHierarchy(String batchNum) {
        MesBatch batch = findBatchByBatchNum(batchNum);

        List<MesOptimizationFile> optimizingFiles = optimizationFileMapper.selectByBatchId(batch.getId());

        List<Long> optimizingFileIds = new ArrayList<>();
        for (MesOptimizationFile file : optimizingFiles) {
            optimizingFileIds.add(file.getId());
        }

        List<MesWorkOrder> workOrders = new ArrayList<>();
        if (!optimizingFileIds.isEmpty()) {
            workOrders = workOrderMapper.selectByOptimizingFileIds(optimizingFileIds);
        }

        BatchHierarchy hierarchy = new BatchHierarchy();
        hierarchy.setBatch(toBatchDTO(batch));
        hierarchy.setOptimizingFiles(buildOptimizingFileHierarchy(optimizingFiles, workOrders));

        return hierarchy;
    }

    @Override
    public PrepackageHierarchy getPrepackageHierarchy(String batchNum, String orderNum, String workId) {
        MesPrepackageOrder prepackageOrder = findPrepackageOrder(batchNum, orderNum, workId);

        PrepackageHierarchy hierarchy = new PrepackageHierarchy();
        PrepackageOrderDTO dto = buildPrepackageHierarchy(prepackageOrder);
        hierarchy.setPrepackageOrder(dto);

        return hierarchy;
    }

    private MesBatch findBatchByBatchNum(String batchNum) {
        MesBatch batch = batchMapper.selectByBatchNum(batchNum);
        if (batch == null) {
            throw new BatchNotFoundException(batchNum);
        }
        return batch;
    }

    private MesPrepackageOrder findPrepackageOrder(String batchNum, String orderNum, String workId) {
        MesPrepackageOrder prepackageOrder = null;
        if (orderNum != null && !orderNum.trim().isEmpty()) {
            prepackageOrder = prepackageOrderMapper.selectByOrderNum(orderNum);
        }
        if (prepackageOrder == null && batchNum != null && !batchNum.trim().isEmpty()
            && workId != null && !workId.trim().isEmpty()) {
            prepackageOrder = prepackageOrderMapper.selectByBatchNumAndWorkId(batchNum, workId);
        }
        if (prepackageOrder == null && workId != null && !workId.trim().isEmpty()) {
            prepackageOrder = prepackageOrderMapper.selectByWorkId(workId);
        }
        if (prepackageOrder == null) {
            String identifier = orderNum != null && !orderNum.trim().isEmpty() ? orderNum : workId;
            throw new PrepackageOrderNotFoundException(identifier);
        }
        return prepackageOrder;
    }

    private List<OptimizingFileDTO> buildOptimizingFileHierarchy(List<MesOptimizationFile> optimizingFiles,
                                                                 List<MesWorkOrder> workOrders) {
        Map<Long, List<MesWorkOrder>> workOrdersByFile = new HashMap<>();
        for (MesWorkOrder workOrder : workOrders) {
            workOrdersByFile
                .computeIfAbsent(workOrder.getOptimizingFileId(), key -> new ArrayList<>())
                .add(workOrder);
        }

        Map<Long, PrepackageOrderDTO> prepackageByWorkOrderId = buildPrepackageByWorkOrderId(workOrders);

        List<OptimizingFileDTO> results = new ArrayList<>();
        for (MesOptimizationFile file : optimizingFiles) {
            OptimizingFileDTO dto = toOptimizingFileDTO(file);
            List<MesWorkOrder> fileOrders = workOrdersByFile.getOrDefault(file.getId(), new ArrayList<>());
            List<WorkOrderDTO> workOrderDTOs = new ArrayList<>();
            for (MesWorkOrder workOrder : fileOrders) {
                WorkOrderDTO workOrderDTO = toWorkOrderDTO(workOrder);
                workOrderDTO.setPrepackageOrder(prepackageByWorkOrderId.get(workOrder.getId()));
                workOrderDTOs.add(workOrderDTO);
            }
            dto.setWorkOrders(workOrderDTOs);
            results.add(dto);
        }

        return results;
    }

    private Map<Long, PrepackageOrderDTO> buildPrepackageByWorkOrderId(List<MesWorkOrder> workOrders) {
        List<Long> workOrderIds = new ArrayList<>();
        for (MesWorkOrder workOrder : workOrders) {
            workOrderIds.add(workOrder.getId());
        }

        List<MesPrepackageOrder> prepackageOrders = new ArrayList<>();
        if (!workOrderIds.isEmpty()) {
            prepackageOrders = prepackageOrderMapper.selectByWorkOrderIds(workOrderIds);
        }

        return buildPrepackageHierarchyMap(prepackageOrders);
    }

    private PrepackageOrderDTO buildPrepackageHierarchy(MesPrepackageOrder prepackageOrder) {
        List<MesBoxCode> boxes = boxCodeMapper.selectByPrepackageOrderIds(
            java.util.Collections.singletonList(prepackageOrder.getId())
        );

        List<Long> boxIds = new ArrayList<>();
        for (MesBoxCode box : boxes) {
            boxIds.add(box.getId());
        }

        List<MesPackage> packages = new ArrayList<>();
        if (!boxIds.isEmpty()) {
            packages = packageMapper.selectByBoxIds(boxIds);
        }

        List<Long> packageIds = new ArrayList<>();
        for (MesPackage pkg : packages) {
            packageIds.add(pkg.getId());
        }

        List<MesBoard> parts = new ArrayList<>();
        if (!packageIds.isEmpty()) {
            parts = boardMapper.selectByPackageIds(packageIds);
        }

        Map<String, List<WorkReportDTO>> workReportsByPart = buildWorkReportsByPartCode(parts);
        Map<Long, List<PartDTO>> partsByPackageId = new HashMap<>();
        for (MesBoard part : parts) {
            PartDTO partDTO = toPartDTO(part);
            partDTO.setWorkReports(workReportsByPart.getOrDefault(part.getPartCode(), new ArrayList<>()));
            partsByPackageId
                .computeIfAbsent(part.getPackageId(), key -> new ArrayList<>())
                .add(partDTO);
        }

        Map<Long, List<PackageDTO>> packagesByBoxId = new HashMap<>();
        for (MesPackage pkg : packages) {
            PackageDTO dto = toPackageDTO(pkg);
            dto.setParts(partsByPackageId.getOrDefault(pkg.getId(), new ArrayList<>()));
            packagesByBoxId
                .computeIfAbsent(pkg.getBoxId(), key -> new ArrayList<>())
                .add(dto);
        }

        Map<Long, List<BoxDTO>> boxesByPrepackageId = new HashMap<>();
        for (MesBoxCode box : boxes) {
            BoxDTO dto = toBoxDTO(box);
            dto.setPackages(packagesByBoxId.getOrDefault(box.getId(), new ArrayList<>()));
            boxesByPrepackageId
                .computeIfAbsent(box.getPrepackageOrderId(), key -> new ArrayList<>())
                .add(dto);
        }

        PrepackageOrderDTO orderDTO = toPrepackageOrderDTO(prepackageOrder);
        orderDTO.setBoxes(boxesByPrepackageId.getOrDefault(prepackageOrder.getId(), new ArrayList<>()));
        return orderDTO;
    }

    private Map<Long, PrepackageOrderDTO> buildPrepackageHierarchyMap(List<MesPrepackageOrder> prepackageOrders) {
        List<Long> prepackageIds = new ArrayList<>();
        for (MesPrepackageOrder order : prepackageOrders) {
            prepackageIds.add(order.getId());
        }

        List<MesBoxCode> boxes = new ArrayList<>();
        if (!prepackageIds.isEmpty()) {
            boxes = boxCodeMapper.selectByPrepackageOrderIds(prepackageIds);
        }

        List<Long> boxIds = new ArrayList<>();
        for (MesBoxCode box : boxes) {
            boxIds.add(box.getId());
        }

        List<MesPackage> packages = new ArrayList<>();
        if (!boxIds.isEmpty()) {
            packages = packageMapper.selectByBoxIds(boxIds);
        }

        List<Long> packageIds = new ArrayList<>();
        for (MesPackage pkg : packages) {
            packageIds.add(pkg.getId());
        }

        List<MesBoard> parts = new ArrayList<>();
        if (!packageIds.isEmpty()) {
            parts = boardMapper.selectByPackageIds(packageIds);
        }

        Map<String, List<WorkReportDTO>> workReportsByPart = buildWorkReportsByPartCode(parts);
        Map<Long, List<PartDTO>> partsByPackageId = new HashMap<>();
        for (MesBoard part : parts) {
            PartDTO partDTO = toPartDTO(part);
            partDTO.setWorkReports(workReportsByPart.getOrDefault(part.getPartCode(), new ArrayList<>()));
            partsByPackageId
                .computeIfAbsent(part.getPackageId(), key -> new ArrayList<>())
                .add(partDTO);
        }

        Map<Long, List<PackageDTO>> packagesByBoxId = new HashMap<>();
        for (MesPackage pkg : packages) {
            PackageDTO dto = toPackageDTO(pkg);
            dto.setParts(partsByPackageId.getOrDefault(pkg.getId(), new ArrayList<>()));
            packagesByBoxId
                .computeIfAbsent(pkg.getBoxId(), key -> new ArrayList<>())
                .add(dto);
        }

        Map<Long, List<BoxDTO>> boxesByPrepackageId = new HashMap<>();
        for (MesBoxCode box : boxes) {
            BoxDTO dto = toBoxDTO(box);
            dto.setPackages(packagesByBoxId.getOrDefault(box.getId(), new ArrayList<>()));
            boxesByPrepackageId
                .computeIfAbsent(box.getPrepackageOrderId(), key -> new ArrayList<>())
                .add(dto);
        }

        Map<Long, PrepackageOrderDTO> result = new HashMap<>();
        for (MesPrepackageOrder order : prepackageOrders) {
            PrepackageOrderDTO dto = toPrepackageOrderDTO(order);
            dto.setBoxes(boxesByPrepackageId.getOrDefault(order.getId(), new ArrayList<>()));
            result.put(order.getWorkOrderId(), dto);
        }

        return result;
    }

    private Map<String, List<WorkReportDTO>> buildWorkReportsByPartCode(List<MesBoard> parts) {
        List<String> partCodes = new ArrayList<>();
        for (MesBoard part : parts) {
            if (part.getPartCode() != null) {
                partCodes.add(part.getPartCode());
            }
        }

        List<MesWorkReport> workReports = new ArrayList<>();
        if (!partCodes.isEmpty()) {
            workReports = workReportMapper.selectByPartCodes(partCodes);
        }

        Map<String, List<WorkReportDTO>> result = new HashMap<>();
        for (MesWorkReport report : workReports) {
            WorkReportDTO dto = toWorkReportDTO(report);
            result.computeIfAbsent(report.getPartCode(), key -> new ArrayList<>()).add(dto);
        }

        return result;
    }

    private BatchDTO toBatchDTO(MesBatch batch) {
        BatchDTO dto = new BatchDTO();
        dto.setId(batch.getId());
        dto.setBatchNum(batch.getBatchNum());
        dto.setBatchType(batch.getBatchType());
        dto.setProductTime(batch.getProductTime());
        dto.setNestingTime(batch.getNestingTime());
        dto.setSimpleBatchNum(batch.getSimpleBatchNum());
        dto.setYmba014(batch.getYmba014());
        dto.setYmba016(batch.getYmba016());
        dto.setIsDeleted(batch.getIsDeleted());
        dto.setCreatedBy(batch.getCreatedBy());
        dto.setCreatedTime(batch.getCreatedTime());
        dto.setUpdatedBy(batch.getUpdatedBy());
        dto.setUpdatedTime(batch.getUpdatedTime());
        return dto;
    }

    private OptimizingFileDTO toOptimizingFileDTO(MesOptimizationFile file) {
        OptimizingFileDTO dto = new OptimizingFileDTO();
        dto.setId(file.getId());
        dto.setBatchId(file.getBatchId());
        dto.setBatchNum(file.getBatchNum());
        dto.setOptimizingFileName(file.getOptimizingFileName());
        dto.setStationCode(file.getStationCode());
        dto.setUrgency(file.getUrgency());
        dto.setIsDeleted(file.getIsDeleted());
        dto.setCreatedBy(file.getCreatedBy());
        dto.setCreatedTime(file.getCreatedTime());
        dto.setUpdatedBy(file.getUpdatedBy());
        dto.setUpdatedTime(file.getUpdatedTime());
        return dto;
    }

    private WorkOrderDTO toWorkOrderDTO(MesWorkOrder workOrder) {
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setId(workOrder.getId());
        dto.setBatchId(workOrder.getBatchId());
        dto.setOptimizingFileId(workOrder.getOptimizingFileId());
        dto.setBatchNum(workOrder.getBatchNum());
        dto.setWorkId(workOrder.getWorkId());
        dto.setRoute(workOrder.getRoute());
        dto.setRouteId(workOrder.getRouteId());
        dto.setOrderType(workOrder.getOrderType());
        dto.setDeliveryTime(workOrder.getDeliveryTime());
        dto.setNestingTime(workOrder.getNestingTime());
        dto.setYmba014(workOrder.getYmba014());
        dto.setYmba015(workOrder.getYmba015());
        dto.setYmba016(workOrder.getYmba016());
        dto.setPart0(workOrder.getPart0());
        dto.setCondition0(workOrder.getCondition0());
        dto.setPartTime0(workOrder.getPartTime0());
        dto.setZuz(workOrder.getZuz());
        dto.setPrepackageStatus(workOrder.getPrepackageStatus());
        dto.setRetryCount(workOrder.getRetryCount());
        dto.setLastPullTime(workOrder.getLastPullTime());
        dto.setErrorMessage(workOrder.getErrorMessage());
        dto.setIsDeleted(workOrder.getIsDeleted());
        dto.setCreatedBy(workOrder.getCreatedBy());
        dto.setCreatedTime(workOrder.getCreatedTime());
        dto.setUpdatedBy(workOrder.getUpdatedBy());
        dto.setUpdatedTime(workOrder.getUpdatedTime());
        return dto;
    }

    private PrepackageOrderDTO toPrepackageOrderDTO(MesPrepackageOrder order) {
        PrepackageOrderDTO dto = new PrepackageOrderDTO();
        dto.setId(order.getId());
        dto.setWorkOrderId(order.getWorkOrderId());
        dto.setBatchId(order.getBatchId());
        dto.setBatchNum(order.getBatchNum());
        dto.setWorkId(order.getWorkId());
        dto.setOrderNum(order.getOrderNum());
        dto.setConsignor(order.getConsignor());
        dto.setContractNo(order.getContractNo());
        dto.setWorkNum(order.getWorkNum());
        dto.setReceiver(order.getReceiver());
        dto.setPhone(order.getPhone());
        dto.setShipBatch(order.getShipBatch());
        dto.setInstallAddress(order.getInstallAddress());
        dto.setCustomer(order.getCustomer());
        dto.setCustomerName(order.getCustomerName());
        dto.setReceiveRegion(order.getReceiveRegion());
        dto.setSpace(order.getSpace());
        dto.setPackType(order.getPackType());
        dto.setProductType(order.getProductType());
        dto.setType(order.getType());
        dto.setFdd8(order.getFdd8());
        dto.setPrepackageInfoSize(order.getPrepackageInfoSize());
        dto.setTotalSet(order.getTotalSet());
        dto.setMaxPackageNo(order.getMaxPackageNo());
        dto.setProductionNum(order.getProductionNum());
        dto.setIsProject(order.getIsProject());
        dto.setFnumber(order.getFnumber());
        dto.setDob(order.getDob());
        dto.setDetailedAddress(order.getDetailedAddress());
        dto.setIsDeleted(order.getIsDeleted());
        dto.setCreatedBy(order.getCreatedBy());
        dto.setCreatedTime(order.getCreatedTime());
        dto.setUpdatedBy(order.getUpdatedBy());
        dto.setUpdatedTime(order.getUpdatedTime());
        return dto;
    }

    private BoxDTO toBoxDTO(MesBoxCode box) {
        BoxDTO dto = new BoxDTO();
        dto.setId(box.getId());
        dto.setPrepackageOrderId(box.getPrepackageOrderId());
        dto.setBatchNum(box.getBatchNum());
        dto.setWorkId(box.getWorkId());
        dto.setBoxCode(box.getBoxCode());
        dto.setBuilding(box.getBuilding());
        dto.setHouse(box.getHouse());
        dto.setRoom(box.getRoom());
        dto.setSetno(box.getSetno());
        dto.setColor(box.getColor());
        dto.setUnit(box.getUnit());
        dto.setIsDeleted(box.getIsDeleted());
        dto.setCreatedBy(box.getCreatedBy());
        dto.setCreatedTime(box.getCreatedTime());
        dto.setUpdatedBy(box.getUpdatedBy());
        dto.setUpdatedTime(box.getUpdatedTime());
        return dto;
    }

    private PackageDTO toPackageDTO(MesPackage pkg) {
        PackageDTO dto = new PackageDTO();
        dto.setId(pkg.getId());
        dto.setBoxId(pkg.getBoxId());
        dto.setBatchNum(pkg.getBatchNum());
        dto.setWorkId(pkg.getWorkId());
        dto.setBoxCode(pkg.getBoxCode());
        dto.setPackageNo(pkg.getPackageNo());
        dto.setLength(pkg.getLength());
        dto.setWidth(pkg.getWidth());
        dto.setDepth(pkg.getDepth());
        dto.setWeight(pkg.getWeight());
        dto.setPartCount(pkg.getPartCount());
        dto.setBoxType(pkg.getBoxType());
        dto.setBoxType2(pkg.getBoxType2());
        dto.setIsDeleted(pkg.getIsDeleted());
        dto.setCreatedBy(pkg.getCreatedBy());
        dto.setCreatedTime(pkg.getCreatedTime());
        dto.setUpdatedBy(pkg.getUpdatedBy());
        dto.setUpdatedTime(pkg.getUpdatedTime());
        return dto;
    }

    private PartDTO toPartDTO(MesBoard part) {
        PartDTO dto = new PartDTO();
        dto.setId(part.getId());
        dto.setPackageId(part.getPackageId());
        dto.setBoxId(part.getBoxId());
        dto.setBatchNum(part.getBatchNum());
        dto.setWorkId(part.getWorkId());
        dto.setPartCode(part.getPartCode());
        dto.setLayer(part.getLayer());
        dto.setPiece(part.getPiece());
        dto.setItemCode(part.getItemCode());
        dto.setItemName(part.getItemName());
        dto.setMatName(part.getMatName());
        dto.setItemLength(part.getItemLength());
        dto.setItemWidth(part.getItemWidth());
        dto.setItemDepth(part.getItemDepth());
        dto.setXAxis(part.getXAxis());
        dto.setYAxis(part.getYAxis());
        dto.setZAxis(part.getZAxis());
        dto.setSortOrder(part.getSortOrder());
        dto.setStandardCode(part.getStandardCode());
        dto.setRotate(part.getRotate());
        dto.setProcessCode(part.getProcessCode());
        dto.setStandardList(part.getStandardList());
        dto.setRealPackageNo(part.getRealPackageNo());
        dto.setIsDeleted(part.getIsDeleted());
        dto.setCreatedBy(part.getCreatedBy());
        dto.setCreatedTime(part.getCreatedTime());
        dto.setUpdatedBy(part.getUpdatedBy());
        dto.setUpdatedTime(part.getUpdatedTime());
        return dto;
    }

    private WorkReportDTO toWorkReportDTO(MesWorkReport report) {
        WorkReportDTO dto = new WorkReportDTO();
        dto.setId(report.getId());
        dto.setWorkId(report.getWorkId());
        dto.setPartCode(report.getPartCode());
        dto.setPartStatus(report.getPartStatus());
        dto.setStationCode(report.getStationCode());
        dto.setStationName(report.getStationName());
        dto.setReportTime(report.getReportTime());
        dto.setIsDeleted(report.getIsDeleted());
        dto.setCreatedBy(report.getCreatedBy());
        dto.setCreatedTime(report.getCreatedTime());
        dto.setUpdatedBy(report.getUpdatedBy());
        dto.setUpdatedTime(report.getUpdatedTime());
        return dto;
    }
}
