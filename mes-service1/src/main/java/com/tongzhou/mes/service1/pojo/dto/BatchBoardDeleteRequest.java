package com.tongzhou.mes.service1.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 批次工单板件删除请求
 */
@Data
public class BatchBoardDeleteRequest {

    @NotBlank(message = "批次号不能为空")
    @Schema(description = "批次号", required = true, example = "BATCH-20260513-001")
    private String batchNum;

    @NotBlank(message = "工单号不能为空")
    @Schema(description = "工单号", required = true, example = "WO-001")
    private String workId;
}
