package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.dto.SysDocDTO;
import com.rxas400adm.system.entity.SysDoc;
import com.rxas400adm.system.mapper.SysDocMapper;
import com.rxas400adm.system.vo.SysDocVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysDocServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysDoc.class);
    }

    @Mock
    private SysDocMapper sysDocMapper;

    private SysDocService service;

    @BeforeEach
    void setUp() {
        service = new SysDocService(sysDocMapper);
    }

    private SysDoc sampleDoc(Long id, String status) {
        SysDoc doc = new SysDoc();
        doc.setId(id);
        doc.setTitle("测试文档");
        doc.setContent("# 内容");
        doc.setCategory("运维");
        doc.setTags("tag1,tag2");
        doc.setStatus(status);
        doc.setCreatedBy("admin");
        doc.setCreatedTime(LocalDateTime.now());
        doc.setUpdatedTime(LocalDateTime.now());
        return doc;
    }

    @Test
    @DisplayName("create → 默认状态 DRAFT，标题去空白")
    void create_shouldDefaultDraft() {
        when(sysDocMapper.insert(any(SysDoc.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysDoc.class).setId(1L);
            return 1;
        });

        SysDocDTO dto = new SysDocDTO();
        dto.setTitle("  新文档  ");
        dto.setContent("内容");
        dto.setCategory(" 运维 ");

        SysDocVO result = service.create(dto, "admin");

        assertEquals("新文档", result.title());
        ArgumentCaptor<SysDoc> captor = ArgumentCaptor.forClass(SysDoc.class);
        verify(sysDocMapper).insert(captor.capture());
        assertEquals("DRAFT", captor.getValue().getStatus());
        assertEquals("运维", captor.getValue().getCategory());
    }

    @Test
    @DisplayName("detail → 文档不存在抛 NOT_FOUND")
    void detail_notFound_shouldThrow() {
        when(sysDocMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.detail(999L));
    }

    @Test
    @DisplayName("detail → 文档存在返回 VO")
    void detail_found_shouldReturnVO() {
        when(sysDocMapper.selectById(1L)).thenReturn(sampleDoc(1L, "DRAFT"));
        SysDocVO result = service.detail(1L);
        assertEquals("测试文档", result.title());
    }

    @Test
    @DisplayName("update → 文档不存在抛 NOT_FOUND")
    void update_notFound_shouldThrow() {
        when(sysDocMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.update(999L, new SysDocDTO(), "admin"));
    }

    @Test
    @DisplayName("update → 部分字段更新（content/tags/status 为 null 时保留原值）")
    void update_partialFields_shouldPreserveOld() {
        SysDoc existing = sampleDoc(1L, "DRAFT");
        when(sysDocMapper.selectById(1L)).thenReturn(existing);

        SysDocDTO dto = new SysDocDTO();
        dto.setTitle("新标题");
        // content/category/tags/status 均为 null → 保留原值

        SysDocVO result = service.update(1L, dto, "admin");
        assertEquals("新标题", result.title());
        assertEquals("# 内容", existing.getContent()); // 保留
        assertEquals("DRAFT", existing.getStatus()); // 保留
        verify(sysDocMapper).updateById(existing);
    }

    @Test
    @DisplayName("update → status 字段可修改")
    void update_statusChange_shouldApply() {
        SysDoc existing = sampleDoc(1L, "DRAFT");
        when(sysDocMapper.selectById(1L)).thenReturn(existing);

        SysDocDTO dto = new SysDocDTO();
        dto.setTitle("t");
        dto.setStatus("PUBLISHED");

        service.update(1L, dto, "admin");
        assertEquals("PUBLISHED", existing.getStatus());
    }

    @Test
    @DisplayName("delete → 文档不存在抛 NOT_FOUND")
    void delete_notFound_shouldThrow() {
        when(sysDocMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.delete(999L));
    }

    @Test
    @DisplayName("delete → 文档存在则删除")
    void delete_found_shouldDelete() {
        when(sysDocMapper.selectById(1L)).thenReturn(sampleDoc(1L, "DRAFT"));
        service.delete(1L);
        verify(sysDocMapper).deleteById(1L);
    }

    @Test
    @DisplayName("list → 关键字搜索含 title 和 content")
    void list_withKeyword_shouldSearchTitleAndContent() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysDoc> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(List.of());
        when(sysDocMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        service.list("搜索", null, null, 1, 10);

        verify(sysDocMapper).selectPage(any(), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("list → status/category 过滤")
    void list_withFilters_shouldApplyEq() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysDoc> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(List.of());
        when(sysDocMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = service.list(null, "PUBLISHED", "运维", 1, 10);
        assertNotNull(result);
        verify(sysDocMapper).selectPage(any(), any(LambdaQueryWrapper.class));
    }
}