package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsWabpConfigDTO;
import com.rxas400adm.as400.dto.BpcsWabpImportResult;
import com.rxas400adm.as400.vo.BpcsWabpConfigVO;

import java.util.List;

/**
 * ㊽ WABP 自动分货配置接口。
 */
public interface IBpcsWabpService {

    /** 查询配置列表 */
    List<BpcsWabpConfigVO> listConfigs(String cono, int limit);

    /** 查询单条配置 */
    BpcsWabpConfigVO getConfig(String cono, String wh, int dayOfWeek);

    /** 新增配置 */
    void createConfig(String cono, BpcsWabpConfigDTO dto);

    /** 更新配置 */
    void updateConfig(String cono, String wh, int dayOfWeek, BpcsWabpConfigDTO dto);

    /** 删除配置 */
    void deleteConfig(String cono, String wh, int dayOfWeek);

    /** Excel 导入 */
    BpcsWabpImportResult importConfigs(String cono, List<BpcsWabpConfigDTO> list);

    /** Excel 导出数据 */
    List<BpcsWabpConfigVO> exportConfigs(String cono);
}
