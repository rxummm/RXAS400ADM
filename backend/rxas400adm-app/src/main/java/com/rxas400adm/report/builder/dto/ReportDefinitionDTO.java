package com.rxas400adm.report.builder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建/更新报表定义 DTO。
 */
@Data
public class ReportDefinitionDTO {

    @NotBlank(message = "报表名称不能为空")
    private String name;

    @NotBlank(message = "数据源不能为空")
    private String dataSource;

    private String title;

    @NotBlank(message = "选中列不能为空")
    private String columnsJson;

    private String filtersJson;

    private String sortsJson;
}
