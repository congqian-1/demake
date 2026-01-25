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

package com.tongzhou.mes.service1.service;

/**
 * 邮件通知服务接口
 * 
 * @author MES Team
 */
public interface EmailNotificationService {

    /**
     * 发送预包装数据拉取失败通知
     * 
     * @param batchNo 批次号
     * @param workOrderNo 工单号
     * @param failureReason 失败原因
     * @param retryCount 重试次数
     */
    void sendPrepackagePullFailureNotification(String batchNo, String workOrderNo, 
            String failureReason, int retryCount);

    /**
     * 发送批次接收通知
     * 
     * @param batchNo 批次号
     * @param workOrderCount 工单数量
     */
    void sendBatchReceiveNotification(String batchNo, int workOrderCount);
}
