package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统值修改请求体（替代 Map&lt;String, String&gt;）。
 */
@Data
public class SystemValueUpdateDTO {
    @NotBlank(message = "系统值不能为空")
    private String value;
}
