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

import com.tongzhou.mes.service1.exception.DuplicateInsertException;
import com.tongzhou.mes.service1.pojo.bo.BatchSaveResult;
import com.tongzhou.mes.service1.pojo.bo.SyncPullResult;
import com.tongzhou.mes.service1.pojo.dto.BatchPushSyncRequest;
import com.tongzhou.mes.service1.pojo.dto.BatchPushSyncResponse;
import com.tongzhou.mes.service1.service.BatchService;
import com.tongzhou.mes.service1.service.BatchSyncService;
import com.tongzhou.mes.service1.service.PrePackageService;
import com.tongzhou.mes.service1.util.UniqueConstraintErrorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 同步批次推送服务实现
 */
@Service
@RequiredArgsConstructor
public class BatchSyncServiceImpl implements BatchSyncService {

    private final BatchService batchService;
    private final PrePackageService prePackageService;

    @Override
    public BatchPushSyncResponse pushAndSync(BatchPushSyncRequest request) {
        return processSyncRequest(request);
    }

    private BatchPushSyncResponse processSyncRequest(BatchPushSyncRequest request) {
        BatchSaveResult saveResult = batchService.saveBatchWithResult(request);
        String batchNum = saveResult.getBatchNum();

        BatchPushSyncResponse response = new BatchPushSyncResponse();
        response.setBatchNum(batchNum);

        Set<String> workIds = new LinkedHashSet<>(saveResult.getTargetedWorkIds());
        response.setTotalWorkOrders(workIds.size());

        for (String workId : workIds) {
            BatchPushSyncResponse.WorkOrderResult workOrderResult = executeSingleWorkOrder(batchNum, workId);
            response.getWorkOrders().add(workOrderResult);
            response.setTotalBoardCount(response.getTotalBoardCount() + Math.max(0, workOrderResult.getBoardCount()));

            if ("PULLED".equals(workOrderResult.getStatus()) || "NO_DATA".equals(workOrderResult.getStatus())) {
                response.setSuccessCount(response.getSuccessCount() + 1);
            } else if ("PROCESSING".equals(workOrderResult.getStatus())) {
                response.setProcessingCount(response.getProcessingCount() + 1);
            } else {
                response.setFailedCount(response.getFailedCount() + 1);
            }
        }

        response.setFinishedAt(LocalDateTime.now());
        response.setSuccess(response.getFailedCount() == 0);
        response.setMessage(response.isSuccess() ? "同步批次推送处理完成" : "同步批次推送处理完成（存在失败项）");
        return response;
    }

    private BatchPushSyncResponse.WorkOrderResult executeSingleWorkOrder(String batchNum, String workId) {
        BatchPushSyncResponse.WorkOrderResult result = new BatchPushSyncResponse.WorkOrderResult();
        result.setWorkId(workId);
        try {
            SyncPullResult pullResult = prePackageService.pullSingleWorkOrderForSync(batchNum, workId);
            result.setStatus(pullResult.getStatus());
            result.setBoardCount(pullResult.getBoardCount());
            result.setErrorCode(pullResult.getErrorCode());
            result.setErrorMessage(pullResult.getErrorMessage());
            if ("FAILED".equals(result.getStatus()) && result.getErrorMessage() != null) {
                normalizeDuplicateError(result);
            }
            return result;
        } catch (DuplicateInsertException e) {
            result.setStatus("FAILED");
            result.setErrorCode(e.getErrorCode());
            result.setErrorMessage(e.getMessage());
            result.setBoardCount(0);
            return result;
        } catch (DuplicateKeyException e) {
            UniqueConstraintErrorMapper.MappedError mappedError = UniqueConstraintErrorMapper.map(e);
            result.setStatus("FAILED");
            result.setErrorCode(mappedError.getErrorCode());
            result.setErrorMessage(mappedError.getErrorMessage());
            result.setBoardCount(0);
            return result;
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setErrorCode("SYNC_PULL_ERROR");
            result.setErrorMessage("同步拉取失败：" + e.getMessage());
            result.setBoardCount(0);
            return result;
        }
    }

    private void normalizeDuplicateError(BatchPushSyncResponse.WorkOrderResult result) {
        String msg = result.getErrorMessage();
        if (msg.contains("工单重复：")) {
            result.setErrorCode("DUP_WORK_ORDER");
            result.setErrorMessage("工单重复：同一批次下工单号已存在，无法重复新增");
            return;
        }
        if (msg.contains("箱码重复：")) {
            result.setErrorCode("DUP_BOX_CODE");
            result.setErrorMessage("箱码重复：同一批次同一工单下箱码已存在，无法重复新增");
            return;
        }
        if (msg.contains("包件重复：")) {
            result.setErrorCode("DUP_PACKAGE_NO");
            result.setErrorMessage("包件重复：同一批次同一工单同一箱码下包号已存在，无法重复新增");
            return;
        }
        if (msg.contains("板件重复：")) {
            result.setErrorCode("DUP_PART_CODE");
            result.setErrorMessage("板件重复：板件编码已存在，无法重复新增");
            return;
        }
        UniqueConstraintErrorMapper.MappedError mappedError =
            UniqueConstraintErrorMapper.map(new RuntimeException(msg));
        if (mappedError.getConstraintName() != null) {
            result.setErrorCode(mappedError.getErrorCode());
            result.setErrorMessage(mappedError.getErrorMessage());
        }
    }
}
