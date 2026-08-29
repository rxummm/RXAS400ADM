package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class UserDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "密码（创建必填，更新时非空则重置）", example = "pass1234")
    private String password;

    @Email(message = "{validation.email}")
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Pattern(regexp = "ACTIVE|DISABLED", message = "{validation.pattern.status}")
    @Schema(description = "状态：ACTIVE / DISABLED", example = "ACTIVE")
    private String status;

    @Schema(description = "角色ID列表（优先）", example = "[1, 2]")
    private List<Long> roleIds;

    @Schema(description = "角色代码列表（回退）", example = "[\"ADMIN\"]")
    private List<String> roleCodes;
}