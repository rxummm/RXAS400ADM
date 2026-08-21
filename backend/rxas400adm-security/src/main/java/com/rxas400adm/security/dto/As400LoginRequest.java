package com.rxas400adm.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AS400 user profile 登录请求：凭据委托 IBM i 认证，成功后自动创建/映射本地用户。
 */
@Data
public class As400LoginRequest {

    @NotNull(message = "{validation.notNull}")
    private Long serverId;

    @NotBlank(message = "{validation.notBlank}")
    private String username;

    @NotBlank(message = "{validation.notBlank}")
    private String password;
}
