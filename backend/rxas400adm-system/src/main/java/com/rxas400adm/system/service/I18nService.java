package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.I18nEntryDTO;
import com.rxas400adm.system.entity.I18nEntry;
import com.rxas400adm.system.mapper.I18nMapper;
import com.rxas400adm.system.vo.I18nEntryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 翻译管理服务（rx_i18n_entry）。i18nMapper 全部收敛于此（R1 分层清零）。
 */
@Service
@RequiredArgsConstructor
public class I18nService implements II18nService {

    private final I18nMapper i18nMapper;

    @Override
    public Map<String, String> translations(String lang) {
        Map<String, String> result = new LinkedHashMap<>();
        i18nMapper.selectList(new LambdaQueryWrapper<I18nEntry>().eq(I18nEntry::getLang, lang))
                .forEach(e -> result.put(e.getI18nKey(), e.getText()));
        return result;
    }

    @Override
    public PageResult<I18nEntryVO> entries(int current, int size, String lang, String keyword) {
        LambdaQueryWrapper<I18nEntry> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(lang)) {
            wrapper.eq(I18nEntry::getLang, lang.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(I18nEntry::getI18nKey, kw).or().like(I18nEntry::getText, kw));
        }
        wrapper.orderByAsc(I18nEntry::getLang).orderByAsc(I18nEntry::getI18nKey);
        Page<I18nEntry> page = i18nMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(I18nEntryVO::from).toList());
    }

    @Override
    public I18nEntryVO save(I18nEntryDTO dto) {
        if (!StringUtils.hasText(dto.getI18nKey()) || !StringUtils.hasText(dto.getLang())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "i18n key 与语言必填");
        }
        dto.setI18nKey(dto.getI18nKey().trim());
        dto.setLang(dto.getLang().trim());
        I18nEntry existing = find(dto.getI18nKey(), dto.getLang());
        if (existing != null) {
            existing.setText(dto.getText());
            i18nMapper.update(existing, new LambdaQueryWrapper<I18nEntry>()
                    .eq(I18nEntry::getI18nKey, dto.getI18nKey())
                    .eq(I18nEntry::getLang, dto.getLang()));
            return I18nEntryVO.from(existing);
        }
        I18nEntry entry = new I18nEntry();
        entry.setI18nKey(dto.getI18nKey());
        entry.setLang(dto.getLang());
        entry.setText(dto.getText());
        i18nMapper.insert(entry);
        return I18nEntryVO.from(entry);
    }

    @Override
    public I18nEntryVO update(I18nEntryDTO dto) {
        I18nEntry existing = find(dto.getI18nKey(), dto.getLang());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "翻译记录不存在");
        }
        existing.setText(dto.getText());
        i18nMapper.update(existing, new LambdaQueryWrapper<I18nEntry>()
                .eq(I18nEntry::getI18nKey, existing.getI18nKey())
                .eq(I18nEntry::getLang, existing.getLang()));
        return I18nEntryVO.from(existing);
    }

    @Override
    public void delete(String lang, String key) {
        i18nMapper.delete(new LambdaQueryWrapper<I18nEntry>()
                .eq(I18nEntry::getLang, lang)
                .eq(I18nEntry::getI18nKey, key));
    }

    private I18nEntry find(String key, String lang) {
        return i18nMapper.selectOne(new LambdaQueryWrapper<I18nEntry>()
                .eq(I18nEntry::getI18nKey, key)
                .eq(I18nEntry::getLang, lang));
    }
}
