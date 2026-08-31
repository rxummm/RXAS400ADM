package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BpcsRcmxConfigDTO {
    @NotBlank(message = "客户代码不能为空")
    private String cust;

    @NotBlank(message = "CSR 工号不能为空")
    private String csrId;

    @NotBlank(message = "状态不能为空")
    private String active;

    private String maintUser;
}