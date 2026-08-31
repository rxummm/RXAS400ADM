package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BpcsWabpConfigDTO {
    @NotBlank(message = "仓库代码不能为空")
    private String wh;

    @NotNull(message = "星期几不能为空")
    private Integer dayOfWeek;

    @NotBlank(message = "时间不能为空")
    private String time;

    @NotBlank(message = "SHPHOLD 不能为空")
    private String shipHold;

    @NotBlank(message = "CRHOLD 不能为空")
    private String crHold;

    @NotBlank(message = "PRHOLD 不能为空")
    private String prHold;

    @NotBlank(message = "状态不能为空")
    private String active;

    @NotBlank(message = "维护人不能为空")
    private String maintUser;
}