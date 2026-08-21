package com.rxas400adm.system.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.IfsClient;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.entity.Doc;
import com.rxas400adm.system.mapper.DocMapper;
import com.rxas400adm.system.vo.DocFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 文档 IFS 存储（从 DocService 拆分）：发布落盘、回收站、恢复、文件读取。
 *
 * <p>V49 文件型文档（PDF/IMAGE）正文为空，内容即 IFS 文件；
 * 类型化文档（MARKDOWN/TEXT/HTML）发布通过时自动落盘到受管目录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocStorageService {

    @org.springframework.beans.factory.annotation.Value("${rxas400.ifs.doc-root:/QOpenSys/rxas400/document}")
    private String docRoot = "/QOpenSys/rxas400/document";

    private final AS400ClientProvider clientProvider;
    private final DocMapper docMapper;

    /**
     * 发布通过时自动落盘 IFS：
     * - 无手动发布路径：写入受管目录 /document/{id}/doc.{ext} 并回填 ifsPath
     * - 已有受管路径：内容变更后重新发布时覆盖刷新
     * - 用户手动指定路径：不覆盖
     */
    public void publishToIfs(Doc doc) {
        String docType = normalizeType(doc.getDocType());
        if (isBinaryType(docType) || !StringUtils.hasText(doc.getContent())) {
            return;
        }
        String managedDir = docRoot + "/" + doc.getId() + "/";
        if (StringUtils.hasText(doc.getIfsPath())) {
            if (doc.getIfsPath().startsWith(managedDir)) {
                try {
                    IfsClient client = clientProvider.current();
                    if (client != null) {
                        client.writeIfsFile(doc.getIfsPath(), doc.getContent());
                    }
                } catch (Exception e) {
                    log.warn("文档重发刷新 IFS 失败(id={}, path={}): {}", doc.getId(), doc.getIfsPath(), e.getMessage());
                }
            }
            return;
        }
        String path = managedDir + "doc." + extForType(docType);
        try {
            IfsClient client = clientProvider.current();
            if (client != null && client.writeIfsFile(path, doc.getContent())) {
                doc.setIfsPath(path);
                docMapper.updateById(doc);
            }
        } catch (Exception e) {
            log.warn("文档发布落盘 IFS 失败(id={}, path={}): {}", doc.getId(), path, e.getMessage());
        }
    }

    /** 移入 IFS 回收站（失败不阻塞 DB 逻辑删除） */
    public void trashIfsQuietly(String path) {
        try {
            IfsClient client = clientProvider.current();
            if (client != null) {
                client.trashIfsFile(path);
            }
        } catch (Exception e) {
            log.warn("IFS 移入回收站失败(path={}): {}", path, e.getMessage());
        }
    }

    /** 从 IFS 回收站恢复（失败不阻塞 DB 恢复） */
    public void restoreIfsQuietly(String originalPath) {
        try {
            IfsClient client = clientProvider.current();
            if (client != null) {
                client.restoreIfsFile(IfsClient.TRASH_ROOT + originalPath);
            }
        } catch (Exception e) {
            log.warn("IFS 恢复失败(path={}): {}", originalPath, e.getMessage());
        }
    }

    /** 读取 IFS 发布文件（V49 文件型预览） */
    public DocFileVO readFile(Long id, String ifsPath) {
        if (!StringUtils.hasText(ifsPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该文档尚未发布到 IFS");
        }
        byte[] bytes = clientProvider.current().readIfsFileBytes(ifsPath);
        if (bytes == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "IFS 文件不存在或不可读: " + ifsPath);
        }
        String path = ifsPath.replace('\\', '/');
        String filename = path.substring(path.lastIndexOf('/') + 1);
        return new DocFileVO(bytes, contentTypeFor(filename), filename);
    }

    /** 文件型文档（正文为空，内容即 IFS 文件） */
    public boolean isBinaryType(String docType) {
        String t = normalizeType(docType);
        return DocService.DOC_TYPE_PDF.equals(t) || DocService.DOC_TYPE_IMAGE.equals(t);
    }

    /** 归一化文档类型：非法值按 MARKDOWN 兜底（共享逻辑，不依赖 DB） */
    private String normalizeType(String docType) {
        if (!StringUtils.hasText(docType)) {
            return DocService.DOC_TYPE_MARKDOWN;
        }
        String upper = docType.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case DocService.DOC_TYPE_TEXT, DocService.DOC_TYPE_HTML,
                 DocService.DOC_TYPE_PDF, DocService.DOC_TYPE_IMAGE -> upper;
            default -> DocService.DOC_TYPE_MARKDOWN;
        };
    }

    /** 按文档类型推导受管目录扩展名 */
    String extForType(String docType) {
        String normalized = normalizeType(docType);
        return switch (normalized) {
            case DocService.DOC_TYPE_TEXT -> "txt";
            case DocService.DOC_TYPE_HTML -> "html";
            default -> "md";
        };
    }

    /** 按扩展名推导 Content-Type（预览用，未知类型回退 octet-stream） */
    public String contentTypeFor(String filename) {
        int dot = filename.lastIndexOf('.');
        String ext = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "md", "markdown" -> "text/markdown; charset=utf-8";
            case "txt", "log", "ini", "conf", "csv" -> "text/plain; charset=utf-8";
            case "html", "htm" -> "text/html; charset=utf-8";
            case "json" -> "application/json; charset=utf-8";
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }
}
