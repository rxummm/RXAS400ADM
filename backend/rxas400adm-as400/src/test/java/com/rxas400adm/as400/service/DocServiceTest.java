package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.as400.dto.DocDTO;
import com.rxas400adm.as400.dto.DocTemplateDTO;
import com.rxas400adm.as400.entity.Doc;
import com.rxas400adm.as400.entity.DocTemplate;
import com.rxas400adm.as400.entity.DocVersion;
import com.rxas400adm.as400.mapper.DocMapper;
import com.rxas400adm.as400.mapper.DocTemplateMapper;
import com.rxas400adm.as400.mapper.DocVersionMapper;
import com.rxas400adm.as400.vo.DocTemplateVO;
import com.rxas400adm.common.exception.BusinessException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Doc.class);
    }

    @Mock
    private DocMapper docMapper;

    @Mock
    private DocTemplateMapper templateMapper;

    @Mock
    private DocVersionMapper versionMapper;

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client as400Client;

    private DocService service;
    private DocTemplateService templateService;
    private DocVersionService versionService;

    @BeforeEach
    void setUp() {
        templateService = new DocTemplateService(templateMapper);
        versionService = new DocVersionService(docMapper, versionMapper);
        service = new DocService(docMapper, templateMapper, templateService, versionService, clientProvider);
    }

    private Doc draftDoc(Long id, String status) {
        Doc doc = new Doc();
        doc.setId(id);
        doc.setTitle("变更单");
        doc.setContent("内容");
        doc.setDocType(DocService.DOC_TYPE_MARKDOWN);
        doc.setVersion(1);
        doc.setStatus(status);
        doc.setCreatedTime(LocalDateTime.now());
        doc.setUpdatedTime(LocalDateTime.now());
        return doc;
    }

    @Test
    void create_shouldSnapshotVersion1() {
        when(docMapper.insert(any(Doc.class))).thenAnswer(inv -> {
            inv.getArgument(0, Doc.class).setId(1L);
            return 1;
        });
        when(versionMapper.selectCount(any())).thenReturn(0L);

        DocDTO draft = new DocDTO();
        draft.setTitle("变更单");
        draft.setContent("内容");
        Doc created = service.createDoc(draft, "admin");

        assertEquals(1, created.getVersion());
        assertEquals(DocService.STATUS_DRAFT, created.getStatus());
        assertEquals(DocService.DOC_TYPE_MARKDOWN, created.getDocType());
        verify(versionMapper).insert(any(DocVersion.class));
    }

    @Test
    void submitApprove_shouldTransitionToPublished() {
        Doc pending = draftDoc(1L, DocService.STATUS_DRAFT);
        when(docMapper.selectById(1L)).thenReturn(pending);

        service.submit(1L, "admin");
        assertEquals(DocService.STATUS_PENDING, pending.getStatus());
        verify(docMapper).updateById(pending);

        when(docMapper.selectById(1L)).thenReturn(draftDoc(1L, DocService.STATUS_PENDING));
        service.approve(1L, "approver");
        Doc approved = docMapper.selectById(1L);
        assertEquals(DocService.STATUS_PUBLISHED, approved.getStatus());
        assertEquals("approver", approved.getApprovedBy());
    }

    @Test
    void approveTextDoc_shouldAutoPublishToIfs() {
        Doc pending = draftDoc(1L, DocService.STATUS_PENDING);
        when(docMapper.selectById(1L)).thenReturn(pending);
        when(clientProvider.current()).thenReturn(as400Client);
        when(as400Client.writeIfsFile(any(String.class), any(String.class))).thenReturn(true);

        service.approve(1L, "approver");

        String expected = "/QOpenSys/rxas400/document/1/doc.md";
        assertEquals(expected, pending.getIfsPath());
    }

    @Test
    void approveBinaryDoc_withoutIfs_shouldThrow() {
        Doc pending = draftDoc(1L, DocService.STATUS_PENDING);
        pending.setDocType(DocService.DOC_TYPE_PDF);
        pending.setContent(null);
        when(docMapper.selectById(1L)).thenReturn(pending);

        assertThrows(BusinessException.class, () -> service.approve(1L, "approver"));
        verify(docMapper, never()).updateById(any(Doc.class));
    }

    @Test
    void reject_shouldRecordReason() {
        when(docMapper.selectById(1L)).thenReturn(draftDoc(1L, DocService.STATUS_PENDING));
        service.reject(1L, "缺少回退方案", "approver");
        Doc rejected = docMapper.selectById(1L);
        assertEquals(DocService.STATUS_REJECTED, rejected.getStatus());
        assertEquals("缺少回退方案", rejected.getRejectReason());
    }

    @Test
    void submitPublished_shouldThrow() {
        when(docMapper.selectById(1L)).thenReturn(draftDoc(1L, DocService.STATUS_PUBLISHED));
        assertThrows(BusinessException.class, () -> service.submit(1L, "admin"));
        verify(docMapper, never()).updateById(any(Doc.class));
    }

    @Test
    void updatePublished_shouldBumpVersionAndSnapshot() {
        when(docMapper.selectById(1L)).thenReturn(draftDoc(1L, DocService.STATUS_PUBLISHED));
        when(versionMapper.selectCount(any())).thenReturn(0L);

        DocDTO update = new DocDTO();
        update.setTitle("变更单 v2");
        update.setContent("新内容");
        Doc result = service.updateDoc(1L, update, "admin");

        assertEquals(2, result.getVersion());
        assertEquals(DocService.STATUS_DRAFT, result.getStatus());
        verify(versionMapper).insert(any(DocVersion.class));
    }

    @Test
    void updatePending_shouldThrow() {
        when(docMapper.selectById(1L)).thenReturn(draftDoc(1L, DocService.STATUS_PENDING));
        DocDTO update = new DocDTO();
        update.setTitle("x");
        assertThrows(BusinessException.class, () -> service.updateDoc(1L, update, "admin"));
    }

    @Test
    void listTemplates_shouldReturnVo() {
        DocTemplate template = new DocTemplate();
        template.setId(1L);
        template.setName("运维变更单");
        template.setCategory("变更管理");
        template.setContent("# ${title}");
        template.setDocType(DocService.DOC_TYPE_MARKDOWN);
        when(templateMapper.selectList(any())).thenReturn(List.of(template));

        List<DocTemplateVO> result = service.listTemplates(null);

        assertEquals(1, result.size());
        assertEquals(DocService.DOC_TYPE_MARKDOWN, result.get(0).docType());
        assertEquals("运维变更单", result.get(0).name());
    }

    @Test
    void updateTemplate_shouldApplyChanges() {
        DocTemplate template = new DocTemplate();
        template.setId(1L);
        template.setName("旧名称");
        template.setContent("旧内容");
        when(templateMapper.selectById(1L)).thenReturn(template);

        DocTemplateDTO dto = new DocTemplateDTO();
        dto.setName("新名称");
        dto.setContent("新内容");
        dto.setDocType(DocService.DOC_TYPE_TEXT);
        service.updateTemplate(1L, dto, "admin");

        assertEquals("新名称", template.getName());
        assertEquals("新内容", template.getContent());
        assertEquals(DocService.DOC_TYPE_TEXT, template.getDocType());
        verify(templateMapper).updateById(template);
    }

    @Test
    void rollback_shouldRestoreSnapshot() {
        Doc published = draftDoc(1L, DocService.STATUS_PUBLISHED);
        when(docMapper.selectById(1L)).thenReturn(published);
        DocVersion version = new DocVersion();
        version.setDocId(1L);
        version.setVersion(1);
        version.setTitle("变更单");
        version.setContent("回滚内容");
        when(versionMapper.selectOne(any())).thenReturn(version);
        when(versionMapper.selectCount(any())).thenReturn(0L);

        service.rollback(1L, 1, "admin");

        assertEquals("回滚内容", published.getContent());
        assertEquals(2, published.getVersion());
        assertEquals(DocService.STATUS_DRAFT, published.getStatus());
    }
}
