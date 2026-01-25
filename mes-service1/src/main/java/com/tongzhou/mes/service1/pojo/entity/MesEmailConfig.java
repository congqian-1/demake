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

package com.tongzhou.mes.service1.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件通知配置表实体类
 * 
 * @author MES Team
 */
@Data
@TableName("mes_email_notification_config")
public class MesEmailConfig {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * SMTP服务器地址
     */
    private String smtpHost;

    /**
     * SMTP端口
     */
    private Integer smtpPort;

    /**
     * 发件人用户名
     */
    private String username;

    /**
     * 发件人密码/授权码
     */
    private String password;

    /**
     * 发件人地址
     */
    private String fromAddress;

    /**
     * 收件人地址（多个用逗号分隔）
     */
    private String toAddresses;

    /**
     * 是否启用（0=禁用/1=启用）
     */
    private Integer enabled;

    /**
     * 逻辑删除标识（0-未删除、1-已删除）
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
