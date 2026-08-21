package com.rxas400adm.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 用户更新请求：username 不可修改；password 非空时重置密码。
 */
@Data
public class UserUpdateDTO {

    /** 非空则重置密码 */
    private String password;

    @Email(message = "{validation.email}")
    private String email;

    /** ACTIVE / DISABLED（启用/禁用） */
    @Pattern(regexp = "ACTIVE|DISABLED", message = "{validation.pattern.status}")
    private String status;

    /** 角色 ID 列表（非 null 时整体重建角色分配，优先） */
    private List<Long> roleIds;

    /** 角色代码列表（回退：roleIds 为空但本字段非空时按 code 重建） */
    private List<String> roleCodes;
}
