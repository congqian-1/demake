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
import com.tongzhou.mes.service1.mapper.MesEmailConfigMapper;
import com.tongzhou.mes.service1.pojo.entity.MesEmailConfig;
import com.tongzhou.mes.service1.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮件通知服务实现类
 * 
 * @author MES Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final JavaMailSender mailSender;
    private final MesEmailConfigMapper emailConfigMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendPrepackagePullFailureNotification(String batchNo, String workOrderNo, 
            String failureReason, int retryCount) {
        try {
            // 查询邮件配置
            MesEmailConfig config = emailConfigMapper.selectOne(
                    new LambdaQueryWrapper<MesEmailConfig>()
                            .eq(MesEmailConfig::getEnabled, 1));
            
            if (config == null) {
                log.warn("未找到启用的数据拉取失败邮件通知配置，跳过发送");
                return;
            }
            
            // 构造邮件内容
            String subject = String.format("[MES告警] 预包装数据拉取失败 - 工单号: %s", workOrderNo);
            String content = buildPrepackagePullFailureContent(batchNo, workOrderNo, failureReason, retryCount);
            
            // 发送邮件
            sendEmail(config.getFromAddress(), config.getToAddresses(), null, subject, content);
            
            log.info("预包装数据拉取失败通知邮件已发送，工单号: {}, 重试次数: {}", workOrderNo, retryCount);
            
        } catch (Exception e) {
            log.error("发送预包装数据拉取失败通知邮件失败，工单号: {}, 错误信息: {}", workOrderNo, e.getMessage(), e);
        }
    }

    @Override
    public void sendBatchReceiveNotification(String batchNo, int workOrderCount) {
        try {
            // 查询邮件配置
            MesEmailConfig config = emailConfigMapper.selectOne(
                    new LambdaQueryWrapper<MesEmailConfig>()
                            .eq(MesEmailConfig::getEnabled, 1));
            
            if (config == null) {
                log.info("未找到启用的批次接收邮件通知配置，跳过发送");
                return;
            }
            
            // 构造邮件内容
            String subject = String.format("[MES通知] 批次数据接收成功 - 批次号: %s", batchNo);
            String content = buildBatchReceiveContent(batchNo, workOrderCount);
            
            // 发送邮件
            sendEmail(config.getFromAddress(), config.getToAddresses(), null, subject, content);
            
            log.info("批次接收通知邮件已发送，批次号: {}, 工单数量: {}", batchNo, workOrderCount);
            
        } catch (Exception e) {
            log.error("发送批次接收通知邮件失败，批次号: {}, 错误信息: {}", batchNo, e.getMessage(), e);
        }
    }

    /**
     * 发送邮件
     */
    private void sendEmail(String from, String recipients, String cc, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();

        if (from != null && !from.trim().isEmpty()) {
            message.setFrom(from.trim());
        }

        // 收件人（多个邮箱用逗号分隔）
        if (recipients != null && !recipients.trim().isEmpty()) {
            message.setTo(recipients.split(","));
        }
        
        // 抄送
        if (cc != null && !cc.trim().isEmpty()) {
            message.setCc(cc.split(","));
        }
        
        message.setSubject(subject);
        message.setText(content);
        message.setSentDate(new java.util.Date());
        
        mailSender.send(message);
    }

    /**
     * 构造预包装数据拉取失败邮件内容
     */
    private String buildPrepackagePullFailureContent(String batchNo, String workOrderNo, 
            String failureReason, int retryCount) {
        StringBuilder content = new StringBuilder();
        content.append("MES系统预包装数据拉取失败告警\n\n");
        content.append("===============================================\n");
        content.append("告警时间: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n");
        content.append("批次号: ").append(batchNo).append("\n");
        content.append("工单号: ").append(workOrderNo).append("\n");
        content.append("重试次数: ").append(retryCount).append("/3\n");
        content.append("失败原因: ").append(failureReason).append("\n");
        content.append("===============================================\n\n");
        
        if (retryCount >= 3) {
            content.append("【重要】该工单已达到最大重试次数(3次)，已标记为\"拉取失败\"状态，请人工介入处理！\n\n");
        } else {
            content.append("系统将自动重试拉取，请关注后续通知。\n\n");
        }
        
        content.append("如需帮助，请联系系统管理员。\n");
        content.append("\n此邮件由MES系统自动发送，请勿回复。");
        
        return content.toString();
    }

    /**
     * 构造批次接收邮件内容
     */
    private String buildBatchReceiveContent(String batchNo, int workOrderCount) {
        StringBuilder content = new StringBuilder();
        content.append("MES系统批次数据接收通知\n\n");
        content.append("===============================================\n");
        content.append("接收时间: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n");
        content.append("批次号: ").append(batchNo).append("\n");
        content.append("工单数量: ").append(workOrderCount).append("\n");
        content.append("===============================================\n\n");
        content.append("批次数据已成功接收并入库，系统将自动拉取预包装数据。\n\n");
        content.append("\n此邮件由MES系统自动发送，请勿回复。");
        
        return content.toString();
    }
}
