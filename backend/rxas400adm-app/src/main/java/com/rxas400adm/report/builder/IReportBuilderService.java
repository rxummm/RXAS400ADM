package com.rxas400adm.report.builder;

import com.rxas400adm.report.builder.dto.ReportDefinitionDTO;
import com.rxas400adm.report.builder.vo.DataSourceMeta;
import com.rxas400adm.report.builder.vo.ReportDefinitionVO;

import java.util.List;
import java.util.Map;

/**
 * 自定义报表构建器服务接口。
 */
public interface IReportBuilderService {

    /** 查询全部数据源元数据（前端字段选择用） */
    List<DataSourceMeta> listDataSources();

    /** 查询全部报表定义 */
    List<ReportDefinitionVO> listDefinitions();

    /** 查询单个报表定义 */
    ReportDefinitionVO getDefinition(Long id);

    /** 创建报表定义 */
    ReportDefinitionVO createDefinition(ReportDefinitionDTO dto, String username);

    /** 更新报表定义 */
    ReportDefinitionVO updateDefinition(Long id, ReportDefinitionDTO dto);

    /** 删除报表定义 */
    void deleteDefinition(Long id);

    /** 执行报表：根据定义查询数据，返回列+行 */
    Map<String, Object> executeReport(Long id);

    /** 导出报表为字节数组（Excel/PDF） */
    byte[] exportReport(Long id, String format);
}
