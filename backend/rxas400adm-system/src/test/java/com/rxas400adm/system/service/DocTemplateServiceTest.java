package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.dto.DocTemplateDTO;
import com.rxas400adm.system.entity.DocTemplate;
import com.rxas400adm.system.mapper.DocTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocTemplateServiceTest {

    @Mock private DocTemplateMapper templateMapper;

    private DocTemplateService service;

    @BeforeEach
    void setUp() {
        service = new DocTemplateService(templateMapper);
    }

    // ---------------- listTemplates ----------------

    @Test
    @DisplayName("listTemplates → 无 category 过滤返回全部")
    void listTemplates_noFilter() {
        when(templateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        var result = service.listTemplates(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listTemplates → 按 category 过滤")
    void listTemplates_withCategory() {
        DocTemplate t = new DocTemplate();
        t.setName("模板1");
        when(templateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t));

        var result = service.listTemplates("运维");
        assertEquals(1, result.size());
        assertEquals("模板1", result.get(0).name());
    }

    // ---------------- createTemplate ----------------

    @Test
    @DisplayName("createTemplate → 正常创建并返回")
    void createTemplate_success() {
        when(templateMapper.insert(any(DocTemplate.class))).thenAnswer(inv -> {
            inv.getArgument(0, DocTemplate.class).setId(10L);
            return 1;
        });

        DocTemplateDTO dto = new DocTemplateDTO();
        dto.setName("模板A");
        dto.setCategory("运维");
        dto.setContent("# content");
        dto.setDocType("MARKDOWN");

        DocTemplate result = service.createTemplate(dto, "admin");

        assertEquals(10L, result.getId());
        assertEquals("模板A", result.getName());
        assertEquals("MARKDOWN", result.getDocType());
        assertEquals("admin", result.getCreatedBy());
        assertNotNull(result.getCreatedTime());
    }

    @Test
    @DisplayName("createTemplate → docType 归一化")
    void createTemplate_normalizesDocType() {
        when(templateMapper.insert(any(DocTemplate.class))).thenAnswer(inv -> {
            inv.getArgument(0, DocTemplate.class).setId(1L);
            return 1;
        });

        DocTemplateDTO dto = new DocTemplateDTO();
        dto.setName("t");
        dto.setDocType("markdown"); // lowercase → normalized

        DocTemplate result = service.createTemplate(dto, "admin");
        assertEquals("MARKDOWN", result.getDocType());
    }

    // ---------------- updateTemplate ----------------

    @Test
    @DisplayName("updateTemplate → 不存在抛 NOT_FOUND")
    void updateTemplate_notFound_shouldThrow() {
        when(templateMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> service.updateTemplate(99L, new DocTemplateDTO(), "admin"));
    }

    @Test
    @DisplayName("updateTemplate → 正常更新")
    void updateTemplate_success() {
        DocTemplate existing = new DocTemplate();
        existing.setId(1L);
        existing.setName("old");
        when(templateMapper.selectById(1L)).thenReturn(existing);
        when(templateMapper.updateById(any(DocTemplate.class))).thenReturn(1);

        DocTemplateDTO dto = new DocTemplateDTO();
        dto.setName("new");
        dto.setCategory("新分类");
        dto.setDocType("HTML");

        service.updateTemplate(1L, dto, "admin");

        assertEquals("new", existing.getName());
        assertEquals("新分类", existing.getCategory());
        assertEquals("HTML", existing.getDocType());
        assertNotNull(existing.getUpdatedTime());
    }

    // ---------------- deleteTemplate ----------------

    @Test
    @DisplayName("deleteTemplate → 正常删除")
    void deleteTemplate() {
        when(templateMapper.deleteById(1L)).thenReturn(1);
        service.deleteTemplate(1L);
        verify(templateMapper).deleteById(1L);
    }

    // ---------------- normalizeType ----------------

    @Test
    @DisplayName("normalizeType → 合法类型原样返回大写")
    void normalizeType_valid() {
        assertEquals("MARKDOWN", service.normalizeType("MARKDOWN"));
        assertEquals("TEXT", service.normalizeType("text"));
        assertEquals("HTML", service.normalizeType("Html"));
        assertEquals("PDF", service.normalizeType("pdf"));
        assertEquals("IMAGE", service.normalizeType("image"));
    }

    @Test
    @DisplayName("normalizeType → null/空/非法值兜底为 MARKDOWN")
    void normalizeType_invalidDefaults() {
        assertEquals("MARKDOWN", service.normalizeType(null));
        assertEquals("MARKDOWN", service.normalizeType(""));
        assertEquals("MARKDOWN", service.normalizeType("unknown"));
    }
}
