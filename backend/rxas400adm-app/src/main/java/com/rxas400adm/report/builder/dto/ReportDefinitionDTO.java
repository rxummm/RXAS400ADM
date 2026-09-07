package com.rxas400adm.report.builder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建/更新报表定义 DTO。
 */
@Data
public class ReportDefinitionDTO {

    @NotBlank(message = "report name is required")
    private String name;

    @NotBlank(message = "data source is required")
    private String dataSource;

    private String title;

    @NotBlank(message = "selected columns are required")
    private String columnsJson;

    private String filtersJson;

    private String sortsJson;
}
