package com.rxas400adm.compile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompileRequest {

    @NotBlank(message = "{validation.notBlank}")
    private String library;

    @NotBlank(message = "{validation.notBlank}")
    private String sourceFile;

    @NotBlank(message = "{validation.notBlank}")
    private String member;

    /** CRTBNDRPG / CRTSQLRPGI / CRTCBLMOD / CRTPGM */
    private String command = "CRTBNDRPG";
}
