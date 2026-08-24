package com.rxas400adm.system.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.entity.Doc;
import com.rxas400adm.system.mapper.DocMapper;
import com.rxas400adm.system.vo.DocFileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocStorageServiceTest {

    @Mock private AS400ClientProvider clientProvider;
    @Mock private AS400Client as400Client;
    @Mock private DocMapper docMapper;

    private DocStorageService service;

    @BeforeEach
    void setUp() {
        service = new DocStorageService(clientProvider, docMapper);
        ReflectionTestUtils.setField(service, "docRoot", "/QOpenSys/rxas400/document");
        lenient().when(clientProvider.current()).thenReturn(as400Client);
    }

    // ---------------- publishToIfs ----------------

    @Test
    @DisplayName("publishToIfs → 二进制类型(PDF)跳过落盘")
    void publishToIfs_binaryType_skips() {
        Doc doc = new Doc();
        doc.setId(1L);
        doc.setDocType("PDF");
        doc.setContent("binary-data");
        service.publishToIfs(doc);
        verify(as400Client, never()).writeIfsFile(anyString(), anyString());
    }

    @Test
    @DisplayName("publishToIfs → 内容为空时跳过")
    void publishToIfs_emptyContent_skips() {
        Doc doc = new Doc();
        doc.setId(1L);
        doc.setDocType("MARKDOWN");
        doc.setContent(null);
        service.publishToIfs(doc);
        verify(as400Client, never()).writeIfsFile(anyString(), anyString());
    }

    @Test
    @DisplayName("publishToIfs → 无 ifsPath 时写入受管目录并回填")
    void publishToIfs_noManagedPath_writesAndBackfills() {
        when(as400Client.writeIfsFile(anyString(), anyString())).thenReturn(true);

        Doc doc = new Doc();
        doc.setId(5L);
        doc.setDocType("MARKDOWN");
        doc.setContent("# Hello");
        service.publishToIfs(doc);

        verify(as400Client).writeIfsFile(eq("/QOpenSys/rxas400/document/5/doc.md"), eq("# Hello"));
        assertEquals("/QOpenSys/rxas400/document/5/doc.md", doc.getIfsPath());
        verify(docMapper).updateById(doc);
    }

    @Test
    @DisplayName("publishToIfs → 已有受管路径且匹配时覆盖刷新")
    void publishToIfs_managedPathMatches_overwrites() {
        when(as400Client.writeIfsFile(anyString(), anyString())).thenReturn(true);

        Doc doc = new Doc();
        doc.setId(5L);
        doc.setDocType("MARKDOWN");
        doc.setContent("# Updated");
        doc.setIfsPath("/QOpenSys/rxas400/document/5/doc.md");
        service.publishToIfs(doc);

        verify(as400Client).writeIfsFile(eq("/QOpenSys/rxas400/document/5/doc.md"), eq("# Updated"));
        // ifsPath already managed, no updateById needed
        verify(docMapper, never()).updateById(any(Doc.class));
    }

    @Test
    @DisplayName("publishToIfs → 已有非受管路径时不覆盖")
    void publishToIfs_customPath_noOverwrite() {
        Doc doc = new Doc();
        doc.setId(5L);
        doc.setDocType("MARKDOWN");
        doc.setContent("# content");
        doc.setIfsPath("/custom/path/doc.md");
        service.publishToIfs(doc);

        verify(clientProvider, never()).current();
    }

    // ---------------- trashIfsQuietly / restoreIfsQuietly ----------------

    @Test
    @DisplayName("trashIfsQuietly → 异常不抛出")
    void trashIfsQuietly_exceptionSwallowed() {
        doThrow(new RuntimeException("IFS error")).when(as400Client).trashIfsFile(anyString());
        assertDoesNotThrow(() -> service.trashIfsQuietly("/some/path"));
    }

    @Test
    @DisplayName("restoreIfsQuietly → 异常不抛出")
    void restoreIfsQuietly_exceptionSwallowed() {
        doThrow(new RuntimeException("IFS error")).when(as400Client).restoreIfsFile(anyString());
        assertDoesNotThrow(() -> service.restoreIfsQuietly("/some/path"));
    }

    // ---------------- readFile ----------------

    @Test
    @DisplayName("readFile → ifsPath 为空抛 NOT_FOUND")
    void readFile_noPath_shouldThrow() {
        assertThrows(BusinessException.class, () -> service.readFile(1L, null));
    }

    @Test
    @DisplayName("readFile → 文件不存在抛 NOT_FOUND")
    void readFile_notExist_shouldThrow() {
        when(as400Client.readIfsFileBytes("/missing")).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.readFile(1L, "/missing"));
    }

    @Test
    @DisplayName("readFile → 正常读取返回 DocFileVO")
    void readFile_success() {
        when(as400Client.readIfsFileBytes("/doc.md")).thenReturn("# content".getBytes());
        DocFileVO result = service.readFile(1L, "/doc.md");
        assertEquals("doc.md", result.filename());
        assertEquals("text/markdown; charset=utf-8", result.contentType());
    }

    // ---------------- contentTypeFor ----------------

    @Test
    @DisplayName("contentTypeFor → 各扩展名映射正确")
    void contentTypeFor_mappings() {
        assertEquals("application/pdf", service.contentTypeFor("file.pdf"));
        assertEquals("image/png", service.contentTypeFor("img.PNG"));
        assertEquals("text/markdown; charset=utf-8", service.contentTypeFor("readme.md"));
        assertEquals("text/plain; charset=utf-8", service.contentTypeFor("data.csv"));
        assertEquals("application/octet-stream", service.contentTypeFor("unknown.xyz"));
    }

    // ---------------- extForType ----------------

    @Test
    @DisplayName("extForType → MARKDOWN→md, TEXT→txt, HTML→html")
    void extForType() {
        assertEquals("md", service.extForType("MARKDOWN"));
        assertEquals("txt", service.extForType("TEXT"));
        assertEquals("html", service.extForType("HTML"));
        assertEquals("md", service.extForType("UNKNOWN"));
    }

    // ---------------- isBinaryType ----------------

    @Test
    @DisplayName("isBinaryType → PDF/IMAGE 为 true, 其他为 false")
    void isBinaryType() {
        assertTrue(service.isBinaryType("PDF"));
        assertTrue(service.isBinaryType("IMAGE"));
        assertFalse(service.isBinaryType("MARKDOWN"));
        assertFalse(service.isBinaryType("TEXT"));
    }
}
