package com.rxas400adm.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "{validation.notBlank}")
    private String username;

    @NotBlank(message = "{validation.notBlank}")
    private String password;
}
