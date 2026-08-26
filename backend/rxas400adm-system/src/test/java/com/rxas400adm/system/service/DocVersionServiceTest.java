package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.entity.Doc;
import com.rxas400adm.system.entity.DocVersion;
import com.rxas400adm.system.mapper.DocMapper;
import com.rxas400adm.system.mapper.DocVersionMapper;
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
class DocVersionServiceTest {

    @Mock private DocMapper docMapper;
    @Mock private DocVersionMapper versionMapper;

    private DocVersionService service;

    @BeforeEach
    void setUp() {
        service = new DocVersionService(docMapper, versionMapper);
    }

    /** 类型安全占位符：利用 any() 的目标类型推断消除裸 Class 字面量的 unchecked 转换警告 */
    private static LambdaQueryWrapper<DocVersion> anyWrapper() {
        return any();
    }

    // ---------------- versions ----------------

    @Test
    @DisplayName("versions → 返回版本列表")
    void versions_returnsList() {
        DocVersion v1 = new DocVersion();
        v1.setVersion(2);
        DocVersion v2 = new DocVersion();
        v2.setVersion(1);
        when(versionMapper.selectList(anyWrapper())).thenReturn(List.of(v1, v2));

        var result = service.versions(1L);
        assertEquals(2, result.size());
    }

    // ---------------- rollback ----------------

    @Test
    @DisplayName("rollback → 文档不存在抛 NOT_FOUND")
    void rollback_docNotFound_shouldThrow() {
        when(docMapper.selectById(1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.rollback(1L, 1, "admin"));
    }

    @Test
    @DisplayName("rollback → 已逻辑删除的文档抛 NOT_FOUND")
    void rollback_docDeleted_shouldThrow() {
        Doc doc = new Doc();
        doc.setId(1L);
        doc.setDeleted(1);
        when(docMapper.selectById(1L)).thenReturn(doc);
        assertThrows(BusinessException.class, () -> service.rollback(1L, 1, "admin"));
    }

    @Test
    @DisplayName("rollback → 审批中不可回滚")
    void rollback_pending_shouldThrow() {
        Doc doc = new Doc();
        doc.setId(1L);
        doc.setStatus("PENDING");
        when(docMapper.selectById(1L)).thenReturn(doc);
        assertThrows(BusinessException.class, () -> service.rollback(1L, 1, "admin"));
    }

    @Test
    @DisplayName("rollback → 版本快照不存在抛 NOT_FOUND")
    void rollback_snapshotNotFound_shouldThrow() {
        Doc doc = new Doc();
        doc.setId(1L);
        doc.setStatus("DRAFT");
        when(docMapper.selectById(1L)).thenReturn(doc);
        when(versionMapper.selectOne(anyWrapper())).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.rollback(1L, 99, "admin"));
    }

    @Test
    @DisplayName("rollback → 草稿回滚恢复内容，不升版本号")
    void rollback_draft_restoresContentNoVersionBump() {
        Doc doc = new Doc();
        doc.setId(1L);
        doc.setStatus("DRAFT");
        doc.setVersion(1);
        doc.setTitle("current");
        when(docMapper.selectById(1L)).thenReturn(doc);

        DocVersion snapshot = new DocVersion();
        snapshot.setTitle("old title");
        snapshot.setContent("old content");
        when(versionMapper.selectOne(anyWrapper())).thenReturn(snapshot);
        when(versionMapper.selectCount(any())).thenReturn(0L);

        service.rollback(1L, 1, "admin");

        assertEquals("old title", doc.getTitle());
        assertEquals("old content", doc.getContent());
        assertEquals(1, doc.getVersion()); // no bump for draft
        assertEquals("DRAFT", doc.getStatus());
        verify(docMapper).updateById(doc);
    }

    @Test
    @DisplayName("rollback → 已发布回滚升版本号并回到草稿")
    void rollback_published_bumpsVersion() {
        Doc doc = new Doc();
        doc.setId(1L);
        doc.setStatus("PUBLISHED");
        doc.setVersion(3);
        when(docMapper.selectById(1L)).thenReturn(doc);

        DocVersion snapshot = new DocVersion();
        snapshot.setTitle("v2 title");
        snapshot.setContent("v2 content");
        when(versionMapper.selectOne(anyWrapper())).thenReturn(snapshot);
        when(versionMapper.selectCount(any())).thenReturn(0L);

        service.rollback(1L, 2, "admin");

        assertEquals(4, doc.getVersion());
        assertEquals("DRAFT", doc.getStatus());
        assertEquals("admin", doc.getUpdatedBy());
    }

    // ---------------- snapshot ----------------

    @Test
    @DisplayName("snapshot → 同版本不重复留档（幂等）")
    void snapshot_idempotent() {
        when(versionMapper.selectCount(any())).thenReturn(1L);
        Doc doc = new Doc();
        doc.setId(1L);
        doc.setVersion(2);
        doc.setTitle("t");
        doc.setContent("c");

        service.snapshot(doc, "admin");

        verify(versionMapper, never()).insert(any(DocVersion.class));
    }

    @Test
    @DisplayName("snapshot → 新版本正常插入")
    void snapshot_inserts() {
        when(versionMapper.selectCount(any())).thenReturn(0L);
        when(versionMapper.insert(any(DocVersion.class))).thenReturn(1);
        Doc doc = new Doc();
        doc.setId(1L);
        doc.setVersion(3);
        doc.setTitle("t");
        doc.setContent("c");

        service.snapshot(doc, "admin");

        ArgumentCaptor<DocVersion> captor = ArgumentCaptor.forClass(DocVersion.class);
        verify(versionMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getDocId());
        assertEquals(3, captor.getValue().getVersion());
        assertEquals("admin", captor.getValue().getOperator());
    }

    // ---------------- deleteVersions ----------------

    @Test
    @DisplayName("deleteVersions → 删除指定文档所有版本")
    void deleteVersions() {
        when(versionMapper.delete(anyWrapper())).thenReturn(5);
        service.deleteVersions(1L);
        verify(versionMapper).delete(anyWrapper());
    }
}
