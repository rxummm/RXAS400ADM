package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.I18nEntryDTO;
import com.rxas400adm.system.vo.I18nEntryVO;

import java.util.Map;

/**
 * 翻译管理服务接口（rx_i18n）。
 */
public interface II18nService {

    /** 前端运行时翻译包（嵌套结构，支持 module 过滤） */
    Map<String, Object> translations(String lang, String module);

    /** 管理端分页查询（语言 / 关键字 / 模块过滤） */
    PageResult<I18nEntryVO> entries(int current, int size, String lang, String keyword, String module);

    /** 新增/覆盖一条翻译（upsert by lang+i18nKey） */
    I18nEntryVO save(I18nEntryDTO dto);

    /** 修改文案 */
    I18nEntryVO update(I18nEntryDTO dto);

    /** 删除一条翻译 */
    void delete(String lang, String key);
}
