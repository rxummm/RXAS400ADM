package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除请求体（替代 Map&lt;String, List&lt;Long&gt;&gt;）。
 */
@Data
public class BatchDeleteDTO {

    @NotEmpty(message = "{validation.notEmpty}")
    @Schema(description = "待删除ID列表", example = "[1, 2, 3]")
    private List<Long> ids;
}