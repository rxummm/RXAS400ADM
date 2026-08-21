package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除请求体（替代 Map&lt;String, List&lt;Long&gt;&gt;）。
 */
@Data
public class BatchDeleteDTO {

    @NotEmpty(message = "{validation.notEmpty}")
    private List<Long> ids;
}
