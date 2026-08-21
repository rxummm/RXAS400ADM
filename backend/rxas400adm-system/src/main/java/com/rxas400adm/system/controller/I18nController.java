package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.I18nEntryDTO;
import com.rxas400adm.system.service.II18nService;
import com.rxas400adm.system.vo.I18nEntryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 翻译管理（rx_i18n_entry）：运行时翻译包（登录即可读）+ 管理端 CRUD（I18N_MANAGE）。
 * R1 分层清零：i18nMapper 已全部下沉至 II18nService（原「Controller 直查 Mapper 唯一例外」废除）。
 */
@RestController
@RequestMapping("/api/v1/i18n")
@RequiredArgsConstructor
@Tag(name = "国际化")
public class I18nController {

    private final II18nService i18nService;

    @GetMapping
    public ApiResponse<Map<String, String>> translations(@RequestParam(defaultValue = "zh-CN") String lang) {
        return ApiResponse.success(i18nService.translations(lang));
    }

    /** 管理端分页查询（语言 / 关键字过滤） */
    @GetMapping("/entries")
    @PreAuthorize("hasAuthority('I18N_MANAGE')")
    public ApiResponse<PageResult<I18nEntryVO>> entries(@RequestParam(defaultValue = "1") int current,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String lang,
                                                      @RequestParam(required = false) String keyword) {
        return ApiResponse.success(i18nService.entries(current, size, lang, keyword));
    }

    /** 新增/覆盖一条翻译 */
    @PostMapping("/entry")
    @PreAuthorize("hasAuthority('I18N_MANAGE')")
    @OperateLog(module = "翻译管理", operation = "新增翻译")
    public ApiResponse<I18nEntryVO> save(@Valid @RequestBody I18nEntryDTO dto) {
        return ApiResponse.success(i18nService.save(dto));
    }

    /** 修改文案 */
    @PutMapping("/entry")
    @PreAuthorize("hasAuthority('I18N_MANAGE')")
    @OperateLog(module = "翻译管理", operation = "修改翻译")
    public ApiResponse<I18nEntryVO> update(@Valid @RequestBody I18nEntryDTO dto) {
        return ApiResponse.success(i18nService.update(dto));
    }

    /** 删除一条翻译 */
    @DeleteMapping("/entry/{lang}/{key}")
    @PreAuthorize("hasAuthority('I18N_MANAGE')")
    @OperateLog(module = "翻译管理", operation = "删除翻译")
    public ApiResponse<Void> delete(@PathVariable String lang, @PathVariable String key) {
        i18nService.delete(lang, key);
        return ApiResponse.success(null);
    }
}
