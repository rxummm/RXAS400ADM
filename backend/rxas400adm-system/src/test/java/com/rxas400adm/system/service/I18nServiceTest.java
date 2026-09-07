package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.dto.I18nEntryDTO;
import com.rxas400adm.system.entity.I18nEntry;
import com.rxas400adm.system.mapper.I18nMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class I18nServiceTest {

    @Mock private I18nMapper i18nMapper;

    private I18nService service;

    @BeforeEach
    void setUp() {
        service = new I18nService(i18nMapper);
    }

    /** 类型安全占位符：利用 any() 的目标类型推断消除裸 Class 字面量的 unchecked 转换警告 */
    private static LambdaQueryWrapper<I18nEntry> anyWrapper() {
        return any();
    }

    // ---------------- translations ----------------

    @Test
    @DisplayName("translations → 按语言返回 key→text 映射")
    void translations_returnsMap() {
        I18nEntry e1 = new I18nEntry();
        e1.setI18nKey("common.search");
        e1.setLang("zh-CN");
        e1.setText("搜索");
        when(i18nMapper.selectList(anyWrapper())).thenReturn(List.of(e1));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = service.translations("zh-CN", null);
        // translations() 返回嵌套 Map，common.search 位于 {common: {search: "搜索"}}
        assertNotNull(result.get("common"));
        @SuppressWarnings("unchecked")
        Map<String, Object> common = (Map<String, Object>) result.get("common");
        assertEquals("搜索", common.get("search"));
    }

    // ---------------- save (upsert) ----------------

    @Test
    @DisplayName("save → key 或 lang 为空则拒绝")
    void save_missingKeyOrLang_shouldThrow() {
        I18nEntryDTO dto = new I18nEntryDTO();
        dto.setI18nKey(" ");
        dto.setLang("zh-CN");
        dto.setText("text");
        assertThrows(BusinessException.class, () -> service.save(dto));
    }

    @Test
    @DisplayName("save → 新增（不存在时插入）")
    void save_insertNew() {
        I18nEntryDTO dto = new I18nEntryDTO();
        dto.setI18nKey("new.key");
        dto.setLang("en-US");
        dto.setText("New");
        when(i18nMapper.selectOne(anyWrapper())).thenReturn(null);
        when(i18nMapper.insert(any(I18nEntry.class))).thenReturn(1);

        var result = service.save(dto);
        assertEquals("new.key", result.i18nKey());
        verify(i18nMapper).insert(any(I18nEntry.class));
        verify(i18nMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("save → 更新（已存在时覆盖 text）")
    void save_updateExisting() {
        I18nEntry existing = new I18nEntry();
        existing.setI18nKey("existing.key");
        existing.setLang("zh-CN");
        existing.setText("old");
        when(i18nMapper.selectOne(anyWrapper())).thenReturn(existing);
        when(i18nMapper.update(any(), any())).thenReturn(1);

        I18nEntryDTO dto = new I18nEntryDTO();
        dto.setI18nKey("existing.key");
        dto.setLang("zh-CN");
        dto.setText("new");
        var result = service.save(dto);

        assertEquals("new", result.text());
        verify(i18nMapper, never()).insert(any(I18nEntry.class));
        verify(i18nMapper).update(any(), any());
    }

    // ---------------- update ----------------

    @Test
    @DisplayName("update → 不存在则抛 NOT_FOUND")
    void update_notFound_shouldThrow() {
        when(i18nMapper.selectOne(anyWrapper())).thenReturn(null);
        I18nEntryDTO dto = new I18nEntryDTO();
        dto.setI18nKey("missing");
        dto.setLang("en-US");
        assertThrows(BusinessException.class, () -> service.update(dto));
    }

    @Test
    @DisplayName("update → 存在时更新 text")
    void update_exists() {
        I18nEntry existing = new I18nEntry();
        existing.setI18nKey("k");
        existing.setLang("en");
        existing.setText("old");
        when(i18nMapper.selectOne(anyWrapper())).thenReturn(existing);
        when(i18nMapper.update(any(), any())).thenReturn(1);

        I18nEntryDTO dto = new I18nEntryDTO();
        dto.setI18nKey("k");
        dto.setLang("en");
        dto.setText("new");
        var result = service.update(dto);
        assertEquals("new", result.text());
    }

    // ---------------- delete ----------------

    @Test
    @DisplayName("delete → 按 lang+key 删除")
    void delete() {
        when(i18nMapper.delete(anyWrapper())).thenReturn(1);
        service.delete("zh-CN", "key");
        verify(i18nMapper).delete(anyWrapper());
    }
}
