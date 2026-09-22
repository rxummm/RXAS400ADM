package com.rxas400adm.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "NCR状态更新请求体")
public class NcrUpdateDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "新状态：OPEN/IN_PROGRESS/CLOSED/CANCELLED")
    private String status;

    @Schema(description = "关闭备注（状态为CLOSED时填写）")
    private String closeRemark;

    @Schema(description = "处置方式：USE_AS_IS/REWORK/SCRAP/RETURN")
    private String disposition;

    @Schema(description = "根本原因")
    private String rootCause;

    @Schema(description = "纠正措施")
    private String correctiveAction;

    @Schema(description = "预防措施")
    private String preventiveAction;

    @Schema(description = "负责人")
    private String assignedTo;
}
