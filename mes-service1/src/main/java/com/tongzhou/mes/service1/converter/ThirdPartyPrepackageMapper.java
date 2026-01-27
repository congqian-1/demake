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

package com.tongzhou.mes.service1.converter;

import com.tongzhou.mes.service1.pojo.dto.PrepackageDataDTO;
import com.tongzhou.mes.service1.pojo.dto.ThirdPartyPrepackageResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 第三方MES新格式 -> 旧PrepackageDataDTO
 */
@Slf4j
@Component
public class ThirdPartyPrepackageMapper {

    public PrepackageDataDTO toPrepackageData(ThirdPartyPrepackageResponseDTO response, String batchNum, String workId) {
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return null;
        }
        if (response.getCode() != null && response.getCode() != 0) {
            log.warn("Third-party response code not success: {}, msg: {}", response.getCode(), response.getMsg());
            return null;
        }

        ThirdPartyPrepackageResponseDTO.DataItem target = chooseDataItem(response.getData(), workId);
        if (target == null || target.getPrePackageInfo() == null || target.getPrePackageInfo().isEmpty()) {
            return null;
        }

        java.util.Map<String, PrepackageDataDTO.BoxInfoDetail> boxByCode = new java.util.LinkedHashMap<>();
        for (ThirdPartyPrepackageResponseDTO.PrePackageInfoItem infoItem : target.getPrePackageInfo()) {
            if (infoItem.getBoxInfoList() == null || infoItem.getBoxInfoList().isEmpty()) {
                continue;
            }
            for (ThirdPartyPrepackageResponseDTO.BoxInfoItem boxInfoItem : infoItem.getBoxInfoList()) {
                String boxCode = boxInfoItem.getBoxCode();
                if (boxCode == null || boxCode.trim().isEmpty()) {
                    boxCode = "UNKNOWN-" + batchNum + "-" + workId;
                }
                PrepackageDataDTO.BoxInfoDetail boxDetail = boxByCode.get(boxCode);
                if (boxDetail == null) {
                    boxDetail = new PrepackageDataDTO.BoxInfoDetail();
                    boxDetail.setBoxCode(boxCode);
                    boxDetail.setSetno(boxInfoItem.getSetno() != null ? boxInfoItem.getSetno() : infoItem.getSetno());
                    boxDetail.setPackageInfos(new ArrayList<>());
                    boxByCode.put(boxCode, boxDetail);
                }
                List<PrepackageDataDTO.PackageInfo> packages = boxDetail.getPackageInfos();
                packages.addAll(buildPackages(boxInfoItem));
            }
        }

        if (boxByCode.isEmpty()) {
            return null;
        }

        PrepackageDataDTO.PrePackageInfo info = new PrepackageDataDTO.PrePackageInfo();
        info.setOrderNum(target.getOrderNum());
        info.setConsignor(target.getConsignor());
        info.setContractNo(target.getContractNo());
        info.setWorkNum(target.getWorkNum());
        info.setReceiver(target.getReceiver());
        info.setPhone(target.getPhone());
        info.setShipBatch(target.getShipBatch());
        info.setInstallAddress(target.getInstallAddress());
        info.setCustomer(target.getCustomer() != null ? target.getCustomer() : target.getCustomerName());
        info.setReceiveRegion(target.getReceiveRegion());
        info.setSpace(target.getSpace());
        info.setPackType(target.getPackType());
        info.setProductType(target.getProductType());
        info.setPrepackageInfoSize(target.getPrePackageInfoSize());
        info.setTotalSet(target.getTotalSet());
        info.setMaxPackageNo(target.getMaxPackageNo());
        info.setProductionNum(null);
        info.setBoxInfoDetails(new ArrayList<>(boxByCode.values()));

        PrepackageDataDTO dto = new PrepackageDataDTO();
        dto.setPrePackageInfo(info);
        return dto;
    }

    private ThirdPartyPrepackageResponseDTO.DataItem chooseDataItem(List<ThirdPartyPrepackageResponseDTO.DataItem> items, String workId) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        if (workId == null || workId.trim().isEmpty()) {
            return items.get(0);
        }
        for (ThirdPartyPrepackageResponseDTO.DataItem item : items) {
            if (workId.equals(item.getWorkNum())) {
                return item;
            }
        }
        return items.get(0);
    }

    private List<PrepackageDataDTO.PackageInfo> buildPackages(ThirdPartyPrepackageResponseDTO.BoxInfoItem boxInfoItem) {
        List<PrepackageDataDTO.PackageInfo> packages = new ArrayList<>();
        if (boxInfoItem == null) {
            return packages;
        }
        PrepackageDataDTO.PackageInfo pkg = new PrepackageDataDTO.PackageInfo();
        pkg.setPackageNo(boxInfoItem.getPackageNo());
        pkg.setLength(boxInfoItem.getLength());
        pkg.setWidth(boxInfoItem.getWidth());
        pkg.setDepth(boxInfoItem.getDepth());
        pkg.setWeight(boxInfoItem.getWeight());
        Integer partCount = boxInfoItem.getPartCount();
        if (partCount == null && boxInfoItem.getPartInfoList() != null) {
            partCount = boxInfoItem.getPartInfoList().size();
        }
        pkg.setPartCount(partCount);
        pkg.setBoxType(boxInfoItem.getBoxType());
        pkg.setPartInfos(buildParts(boxInfoItem.getPartInfoList()));
        packages.add(pkg);
        return packages;
    }

    private List<PrepackageDataDTO.PartInfo> buildParts(List<ThirdPartyPrepackageResponseDTO.PartInfoItem> partInfoList) {
        List<PrepackageDataDTO.PartInfo> parts = new ArrayList<>();
        if (partInfoList == null || partInfoList.isEmpty()) {
            return parts;
        }
        for (ThirdPartyPrepackageResponseDTO.PartInfoItem part : partInfoList) {
            if (part == null || part.getPartCode() == null) {
                continue;
            }
            PrepackageDataDTO.PartInfo dto = new PrepackageDataDTO.PartInfo();
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
            dto.setStandardListJson(null);
            parts.add(dto);
        }
        return parts;
    }
}
