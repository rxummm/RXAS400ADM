package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BpcsRcmxConfigDTO {
    @NotBlank(message = "customer code is required")
    private String cust;

    @NotBlank(message = "CSR ID is required")
    private String csrId;

    @NotBlank(message = "status is required")
    private String active;

    private String maintUser;
}