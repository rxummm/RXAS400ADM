package com.rxas400adm.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 收件人分组创建/更新 DTO。
 */
@Data
public class EmailGroupCreateDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "分组名称", example = "运维团队")
    private String groupName;

    @Schema(description = "分组描述", example = "运维相关人员")
    private String description;
}