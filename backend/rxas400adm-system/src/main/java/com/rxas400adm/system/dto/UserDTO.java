package com.rxas400adm.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class UserDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String username;

    /** 创建必填；更新时非空则重置密码 */
    private String password;

    @Email(message = "{validation.email}")
    private String email;

    /** ACTIVE / DISABLED */
    @Pattern(regexp = "ACTIVE|DISABLED", message = "{validation.pattern.status}")
    private String status;

    /** 角色 ID 列表（角色分配，优先） */
    private List<Long> roleIds;

    /** 角色代码列表（回退：roleIds 为空时按 roleCode 解析为 ID） */
    private List<String> roleCodes;
}
