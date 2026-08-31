package com.rxas400adm.report.builder.vo;

import com.rxas400adm.report.builder.ReportDefinition;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表定义响应 VO。
 */
@Data
public class ReportDefinitionVO {

    private Long id;
    private String name;
    private String dataSource;
    private String title;
    private String columnsJson;
    private String filtersJson;
    private String sortsJson;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static ReportDefinitionVO from(ReportDefinition entity) {
        ReportDefinitionVO vo = new ReportDefinitionVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDataSource(entity.getDataSource());
        vo.setTitle(entity.getTitle());
        vo.setColumnsJson(entity.getColumnsJson());
        vo.setFiltersJson(entity.getFiltersJson());
        vo.setSortsJson(entity.getSortsJson());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }
}
