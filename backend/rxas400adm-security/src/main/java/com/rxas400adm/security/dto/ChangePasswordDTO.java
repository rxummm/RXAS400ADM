package com.rxas400adm.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String oldPassword;

    @NotBlank(message = "{validation.notBlank}")
    @Size(min = 8, message = "{validation.password.length}")
    private String newPassword;
}
