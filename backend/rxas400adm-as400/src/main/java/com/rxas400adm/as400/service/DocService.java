package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.IfsClient;
import com.rxas400adm.as400.dto.DocDTO;
import com.rxas400adm.as400.dto.DocTemplateDTO;
import com.rxas400adm.as400.entity.Doc;
import com.rxas400adm.as400.entity.DocTemplate;
import com.rxas400adm.as400.mapper.DocMapper;
import com.rxas400adm.as400.mapper.DocTemplateMapper;
import com.rxas400adm.as400.vo.DocFileVO;
import com.rxas400adm.as400.vo.DocTemplateVO;
import com.rxas400adm.as400.vo.DocVersionVO;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 运维文档管理 Facade（从 system 模块迁入，合并 DocStorageService）：
 * 模板起草 → 版本管理 → 审批流 → IFS 发布。
 *
 * <p>已拆分为 3 个内部服务，本类为 Facade：
 * <ul>
 *   <li>{@link DocTemplateService} — 模板 CRUD</li>
 *   <li>{@link DocVersionService} — 版本快照 / 回滚 / 历史</li>
 *   <li>IFS 文件存储 — 本类内联（publishToIfs / readFile / trash / restore / sweep）</li>
 * </ul>
 *
 * <p>V49 类型化增强：MARKDOWN / TEXT / HTML / PDF / IMAGE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String DOC_TYPE_MARKDOWN = "MARKDOWN";
    public static final String DOC_TYPE_TEXT = "TEXT";
    public static final String DOC_TYPE_HTML = "HTML";
    public static final String DOC_TYPE_PDF = "PDF";
    public static final String DOC_TYPE_IMAGE = "IMAGE";

    @Value("${rxas400.ifs.doc-root:/QOpenSys/rxas400/document}")
    private String docRoot = "/QOpenSys/rxas400/document";

    private final DocMapper docMapper;
    private final DocTemplateMapper templateMapper;
    private final DocTemplateService templateService;
    private final DocVersionService versionService;
    private final AS400ClientProvider clientProvider;

    /* ---------------- 模板（委托 DocTemplateService） ---------------- */

    public List<DocTemplateVO> listTemplates(String category) {
        return templateService.listTemplates(category);
    }

    public DocTemplate createTemplate(DocTemplateDTO dto, String operator) {
        return templateService.createTemplate(dto, operator);
    }

    public void updateTemplate(Long id, DocTemplateDTO dto, String operator) {
        templateService.updateTemplate(id, dto, operator);
    }

    public void deleteTemplate(Long id) {
        templateService.deleteTemplate(id);
    }

    /* ---------------- 文档 ---------------- */

    public PageResult<Doc> listDocs(String keyword, String status, long current, long size,
                                    boolean deletedOnly) {
        LambdaQueryWrapper<Doc> wrapper = new LambdaQueryWrapper<Doc>()
                .eq(Doc::getDeleted, deletedOnly ? 1 : 0)
                .orderByDesc(deletedOnly ? Doc::getDeletedTime : Doc::getUpdatedTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Doc::getTitle, keyword.trim())
                    .or().like(Doc::getContent, keyword.trim()));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Doc::getStatus, status.trim().toUpperCase());
        }
        Page<Doc> page = docMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        List<Long> templateIds = page.getRecords().stream()
                .map(Doc::getTemplateId)
                .filter(Objects::nonNull)
                .distinct().toList();
        Map<Long, String> templateNames = templateIds.isEmpty() ? Map.of()
                : templateMapper.selectBatchIds(templateIds).stream()
                        .collect(Collectors.toMap(DocTemplate::getId, DocTemplate::getName, (a, b) -> b));
        page.getRecords().forEach(doc -> {
            if (doc.getTemplateId() != null) {
                doc.setTemplateName(templateNames.get(doc.getTemplateId()));
            }
        });
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public Doc createDoc(DocDTO dto, String operator) {
        Doc doc = new Doc();
        doc.setTemplateId(dto.getTemplateId());
        doc.setTitle(dto.getTitle());
        doc.setContent(dto.getContent());
        doc.setDocType(templateService.normalizeType(dto.getDocType()));
        doc.setIfsPath(dto.getIfsPath());
        doc.setId(null);
        doc.setVersion(1);
        doc.setStatus(STATUS_DRAFT);
        doc.setCreatedBy(operator);
        doc.setUpdatedBy(operator);
        doc.setCreatedTime(LocalDateTime.now());
        doc.setUpdatedTime(LocalDateTime.now());
        docMapper.insert(doc);
        versionService.snapshot(doc, operator);
        return doc;
    }

    public Doc updateDoc(Long id, DocDTO update, String operator) {
        Doc doc = requireDoc(id);
        if (STATUS_PENDING.equals(doc.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Document is under review, cannot edit. Please reject or wait for approval");
        }
        doc.setTitle(update.getTitle());
        doc.setContent(update.getContent());
        if (StringUtils.hasText(update.getDocType())) {
            doc.setDocType(templateService.normalizeType(update.getDocType()));
        }
        if (update.getTemplateId() != null) {
            doc.setTemplateId(update.getTemplateId());
        }
        if (StringUtils.hasText(update.getIfsPath())) {
            doc.setIfsPath(update.getIfsPath().trim());
        }
        if (STATUS_PUBLISHED.equals(doc.getStatus())) {
            doc.setVersion((doc.getVersion() == null ? 1 : doc.getVersion()) + 1);
        }
        doc.setStatus(STATUS_DRAFT);
        doc.setUpdatedBy(operator);
        doc.setUpdatedTime(LocalDateTime.now());
        docMapper.updateById(doc);
        versionService.snapshot(doc, operator);
        return doc;
    }

    public void submit(Long id, String operator) {
        Doc doc = requireDoc(id);
        if (!STATUS_DRAFT.equals(doc.getStatus()) && !STATUS_REJECTED.equals(doc.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only draft/rejected documents can be submitted for review");
        }
        doc.setStatus(STATUS_PENDING);
        doc.setRejectReason(null);
        doc.setUpdatedBy(operator);
        doc.setUpdatedTime(LocalDateTime.now());
        docMapper.updateById(doc);
    }

    public void approve(Long id, String operator) {
        Doc doc = requireDoc(id);
        if (!STATUS_PENDING.equals(doc.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only pending-review documents can be approved");
        }
        String docType = templateService.normalizeType(doc.getDocType());
        if (isBinaryType(docType) && !StringUtils.hasText(doc.getIfsPath())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File-type documents (PDF/image) must be uploaded to IFS before publishing");
        }
        doc.setStatus(STATUS_PUBLISHED);
        doc.setApprovedBy(operator);
        doc.setApprovedTime(LocalDateTime.now());
        doc.setUpdatedBy(operator);
        doc.setUpdatedTime(LocalDateTime.now());
        docMapper.updateById(doc);
        publishToIfs(doc);
    }

    public void reject(Long id, String reason, String operator) {
        Doc doc = requireDoc(id);
        if (!STATUS_PENDING.equals(doc.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only pending-review documents can be rejected");
        }
        doc.setStatus(STATUS_REJECTED);
        doc.setRejectReason(reason);
        doc.setUpdatedBy(operator);
        doc.setUpdatedTime(LocalDateTime.now());
        docMapper.updateById(doc);
    }

    public void delete(Long id) {
        Doc doc = requireDoc(id);
        if (STATUS_PENDING.equals(doc.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot delete document under review");
        }
        if (StringUtils.hasText(doc.getIfsPath())) {
            trashIfsQuietly(doc.getIfsPath());
        }
        LocalDateTime now = LocalDateTime.now();
        doc.setDeleted(1);
        doc.setDeletedTime(now);
        doc.setUpdatedTime(now);
        docMapper.updateById(doc);
    }

    public void restore(Long id) {
        Doc doc = docMapper.selectById(id);
        if (doc == null || doc.getDeleted() == null || doc.getDeleted() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Deleted document not found");
        }
        if (StringUtils.hasText(doc.getIfsPath())) {
            restoreIfsQuietly(doc.getIfsPath());
        }
        doc.setDeleted(0);
        doc.setDeletedTime(null);
        doc.setUpdatedTime(LocalDateTime.now());
        docMapper.updateById(doc);
    }

    public void purge(Long id) {
        Doc doc = docMapper.selectById(id);
        if (doc == null || doc.getDeleted() == null || doc.getDeleted() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Deleted document not found");
        }
        docMapper.deleteById(id);
        versionService.deleteVersions(id);
    }

    /* ---------------- 版本（委托 DocVersionService） ---------------- */

    public List<DocVersionVO> versions(Long id) {
        return versionService.versions(id);
    }

    public void rollback(Long id, Integer version, String operator) {
        versionService.rollback(id, version, operator);
    }

    /* ---------------- 文件（IFS 存储，原 DocStorageService） ---------------- */

    public Doc detail(Long id) {
        return requireDoc(id);
    }

    public DocFileVO file(Long id) {
        Doc doc = requireDoc(id);
        return readFile(id, doc.getIfsPath());
    }

    /** 发布通过时自动落盘 IFS */
    private void publishToIfs(Doc doc) {
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
                docMapper.update(null, new LambdaUpdateWrapper<Doc>()
                        .eq(Doc::getId, doc.getId())
                        .and(w -> w.isNull(Doc::getIfsPath).or().eq(Doc::getIfsPath, ""))
                        .set(Doc::getIfsPath, path));
                doc.setIfsPath(path);
            }
        } catch (Exception e) {
            log.warn("文档发布落盘 IFS 失败(id={}, path={}): {}", doc.getId(), path, e.getMessage());
        }
    }

    /** 【T3】IFS 发布补偿 sweep */
    @Scheduled(fixedDelayString = "${rxas400.doc.ifs-resweep-ms:1800000}")
    public void resweepUnpublishedIfsDocs() {
        List<Doc> pending;
        try {
            pending = docMapper.selectList(new LambdaQueryWrapper<Doc>()
                    .eq(Doc::getStatus, STATUS_PUBLISHED)
                    .eq(Doc::getDeleted, 0)
                    .notIn(Doc::getDocType, "PDF", "IMAGE")
                    .isNotNull(Doc::getContent)
                    .ne(Doc::getContent, "")
                    .and(w -> w.isNull(Doc::getIfsPath).or().eq(Doc::getIfsPath, "")));
        } catch (Exception e) {
            log.warn("【T3】IFS 补偿 sweep 查询失败: {}", e.getMessage());
            return;
        }
        if (pending.isEmpty()) {
            return;
        }
        log.info("【T3】IFS 补偿 sweep 开始，待重发布文档 {} 篇", pending.size());
        for (Doc doc : pending) {
            try {
                publishToIfs(doc);
                if (StringUtils.hasText(doc.getIfsPath())) {
                    log.info("【T3】文档补偿发布成功(id={}, path={})", doc.getId(), doc.getIfsPath());
                } else {
                    log.warn("【T3】文档补偿发布未落盘(id={}, type={})，下轮继续", doc.getId(), doc.getDocType());
                }
            } catch (Exception e) {
                log.warn("【T3】文档补偿发布失败(id={}): {}", doc.getId(), e.getMessage());
            }
        }
    }

    /** 移入 IFS 回收站 */
    private void trashIfsQuietly(String path) {
        try {
            IfsClient client = clientProvider.current();
            if (client != null) {
                client.trashIfsFile(path);
            }
        } catch (Exception e) {
            log.warn("IFS 移入回收站失败(path={}): {}", path, e.getMessage());
        }
    }

    /** 从 IFS 回收站恢复 */
    private void restoreIfsQuietly(String originalPath) {
        try {
            IfsClient client = clientProvider.current();
            if (client != null) {
                client.restoreIfsFile(IfsClient.TRASH_ROOT + originalPath);
            }
        } catch (Exception e) {
            log.warn("IFS 恢复失败(path={}): {}", originalPath, e.getMessage());
        }
    }

    /** 读取 IFS 发布文件 */
    private DocFileVO readFile(Long id, String ifsPath) {
        if (!StringUtils.hasText(ifsPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Document not yet published to IFS");
        }
        byte[] bytes = clientProvider.current().readIfsFileBytes(ifsPath);
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "IFS file not found or unreadable: " + ifsPath);
        }
        String path = ifsPath.replace('\\', '/');
        String filename = path.substring(path.lastIndexOf('/') + 1);
        return new DocFileVO(bytes, contentTypeFor(filename), filename);
    }

    /* ---------------- IFS 辅助方法 ---------------- */

    /** 文件型文档（正文为空，内容即 IFS 文件） */
    public boolean isBinaryType(String docType) {
        String t = normalizeType(docType);
        return DOC_TYPE_PDF.equals(t) || DOC_TYPE_IMAGE.equals(t);
    }

    /** 归一化文档类型：非法值按 MARKDOWN 兜底 */
    private String normalizeType(String docType) {
        if (!StringUtils.hasText(docType)) {
            return DOC_TYPE_MARKDOWN;
        }
        String upper = docType.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case DOC_TYPE_TEXT, DOC_TYPE_HTML,
                 DOC_TYPE_PDF, DOC_TYPE_IMAGE -> upper;
            default -> DOC_TYPE_MARKDOWN;
        };
    }

    /** 按文档类型推导受管目录扩展名 */
    private String extForType(String docType) {
        String normalized = normalizeType(docType);
        return switch (normalized) {
            case DOC_TYPE_TEXT -> "txt";
            case DOC_TYPE_HTML -> "html";
            default -> "md";
        };
    }

    /** 按扩展名推导 Content-Type */
    private String contentTypeFor(String filename) {
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

    /* ---------------- 内部辅助 ---------------- */

    private Doc requireDoc(Long id) {
        Doc doc = docMapper.selectById(id);
        if (doc == null || (doc.getDeleted() != null && doc.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Document not found");
        }
        return doc;
    }
}
