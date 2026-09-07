package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BpcsWabpConfigDTO {
    @NotBlank(message = "warehouse code is required")
    private String wh;

    @NotNull(message = "day of week is required")
    private Integer dayOfWeek;

    @NotBlank(message = "time is required")
    private String time;

    @NotBlank(message = "SHPHOLD is required")
    private String shipHold;

    @NotBlank(message = "CRHOLD is required")
    private String crHold;

    @NotBlank(message = "PRHOLD is required")
    private String prHold;

    @NotBlank(message = "status is required")
    private String active;

    @NotBlank(message = "maintainer is required")
    private String maintUser;
}