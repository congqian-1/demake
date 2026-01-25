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
import com.tongzhou.mes.service1.exception.DuplicateWorkReportException;
import com.tongzhou.mes.service1.exception.PartNotFoundException;
import com.tongzhou.mes.service1.mapper.MesBoardMapper;
import com.tongzhou.mes.service1.mapper.MesWorkReportMapper;
import com.tongzhou.mes.service1.pojo.dto.WorkReportRequest;
import com.tongzhou.mes.service1.pojo.entity.MesBoard;
import com.tongzhou.mes.service1.pojo.entity.MesWorkReport;
import com.tongzhou.mes.service1.service.WorkReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 报工记录服务实现
 *
 * @author MES Team
 */
@Slf4j
@Service
public class WorkReportServiceImpl implements WorkReportService {

    @Autowired
    private MesBoardMapper mesBoardMapper;

    @Autowired
    private MesWorkReportMapper mesWorkReportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWorkReport(WorkReportRequest request) {
        log.info("开始处理板件报工，板件码: {}, 状态: {}, 工位: {}", 
            request.getPartCode(), request.getPartStatus(), request.getStationCode());

        // 1. 查询板件信息
        MesBoard part = mesBoardMapper.selectOne(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getPartCode, request.getPartCode())
        );

        if (part == null) {
            log.warn("板件码不存在: {}", request.getPartCode());
            throw new PartNotFoundException(request.getPartCode());
        }

        // 2. 幂等性检查：查询最后一次报工记录
        MesWorkReport lastReport = mesWorkReportMapper.selectOne(
            new LambdaQueryWrapper<MesWorkReport>()
                .eq(MesWorkReport::getPartCode, request.getPartCode())
                .orderByDesc(MesWorkReport::getReportTime)
                .last("LIMIT 1")
        );

        // 如果最后一次报工在同一工位且状态未变化，则拒绝重复报工
        if (lastReport != null
            && request.getStationCode().equals(lastReport.getStationCode())
            && request.getPartStatus().equals(lastReport.getPartStatus())) {
            log.warn("重复报工被拒绝，板件码: {}, 状态: {}", request.getPartCode(), request.getPartStatus());
            throw new DuplicateWorkReportException(request.getPartCode(), request.getPartStatus());
        }

        // 3. 创建报工记录
        MesWorkReport workReport = new MesWorkReport();
        workReport.setWorkId(part.getWorkId());
        workReport.setPartCode(request.getPartCode());
        workReport.setPartStatus(request.getPartStatus());
        workReport.setStationCode(request.getStationCode());
        workReport.setStationName(request.getStationName());
        workReport.setReportTime(LocalDateTime.now());
        workReport.setCreatedBy("SYSTEM");
        workReport.setCreatedTime(LocalDateTime.now());

        // 4. 保存报工记录
        mesWorkReportMapper.insert(workReport);

        log.info("板件报工成功，板件码: {}, 状态: {}, 工位: {}, 操作工: {}", 
            request.getPartCode(), request.getPartStatus(), 
            request.getStationCode(), request.getOperatorName());
    }
}
