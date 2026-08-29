package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 用户更新请求：username 不可修改；password 非空时重置密码。
 */
@Data
public class UserUpdateDTO {

    @Schema(description = "密码（非空则重置）", example = "newpass1234")
    private String password;

    @Email(message = "{validation.email}")
    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Pattern(regexp = "ACTIVE|DISABLED", message = "{validation.pattern.status}")
    @Schema(description = "状态：ACTIVE / DISABLED", example = "ACTIVE")
    private String status;

    @Schema(description = "角色ID列表（非null时整体重建，优先）", example = "[1, 2]")
    private List<Long> roleIds;

    @Schema(description = "角色代码列表（回退：roleIds为空时按code重建）", example = "[\"ADMIN\"]")
    private List<String> roleCodes;
}