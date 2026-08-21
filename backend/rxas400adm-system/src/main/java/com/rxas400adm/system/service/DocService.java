package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.DocDTO;
import com.rxas400adm.system.dto.DocTemplateDTO;
import com.rxas400adm.system.entity.Doc;
import com.rxas400adm.system.entity.DocTemplate;
import com.rxas400adm.system.entity.DocVersion;
import com.rxas400adm.system.mapper.DocMapper;
import com.rxas400adm.system.mapper.DocTemplateMapper;
import com.rxas400adm.system.vo.DocFileVO;
import com.rxas400adm.system.vo.DocTemplateVO;
import com.rxas400adm.system.vo.DocVersionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * 文档管理（3.9）：模板起草 → 版本管理 → 审批流 → 发布。
 *
 * <p>已拆分为 3 个内部服务，本类为 Facade：
 * <ul>
 *   <li>{@link DocTemplateService} — 模板 CRUD</li>
 *   <li>{@link DocVersionService} — 版本快照 / 回滚 / 历史</li>
 *   <li>{@link DocStorageService} — IFS 文件存储（发布/回收站/恢复/读取）</li>
 * </ul>
 *
 * <p>V49 类型化增强：MARKDOWN / TEXT / HTML / PDF / IMAGE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocService implements IDocService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String DOC_TYPE_MARKDOWN = "MARKDOWN";
    public static final String DOC_TYPE_TEXT = "TEXT";
    public static final String DOC_TYPE_HTML = "HTML";
    public static final String DOC_TYPE_PDF = "PDF";
    public static final String DOC_TYPE_IMAGE = "IMAGE";

    private final DocMapper docMapper;
    private final DocTemplateMapper templateMapper;
    private final DocTemplateService templateService;
    private final DocVersionService versionService;
    private final DocStorageService storageService;

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
        // 批量预取模板名，避免 N+1
        List<Long> templateIds = page.getRecords().stream()
                .map(Doc::getTemplateId)
                .filter(java.util.Objects::nonNull)
                .distinct().toList();
        Map<Long, String> templateNames = templateIds.isEmpty() ? Map.of()
                : templateMapper.selectBatchIds(templateIds).stream()
                        .collect(java.util.stream.Collectors.toMap(DocTemplate::getId, DocTemplate::getName));
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
            throw new BusinessException(ErrorCode.FORBIDDEN, "文档审批中不可编辑，请先驳回或等待结果");
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
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅草稿/驳回状态的文档可提交审批");
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
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅审批中的文档可通过");
        }
        String docType = templateService.normalizeType(doc.getDocType());
        if (storageService.isBinaryType(docType) && !StringUtils.hasText(doc.getIfsPath())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件型文档（PDF/图片）须先上传到 IFS 才能发布");
        }
        doc.setStatus(STATUS_PUBLISHED);
        doc.setApprovedBy(operator);
        doc.setApprovedTime(LocalDateTime.now());
        doc.setUpdatedBy(operator);
        doc.setUpdatedTime(LocalDateTime.now());
        docMapper.updateById(doc);
        storageService.publishToIfs(doc);
    }

    public void reject(Long id, String reason, String operator) {
        Doc doc = requireDoc(id);
        if (!STATUS_PENDING.equals(doc.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅审批中的文档可驳回");
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
            throw new BusinessException(ErrorCode.FORBIDDEN, "审批中的文档不可删除");
        }
        if (StringUtils.hasText(doc.getIfsPath())) {
            storageService.trashIfsQuietly(doc.getIfsPath());
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
            throw new BusinessException(ErrorCode.NOT_FOUND, "已删除文档不存在");
        }
        if (StringUtils.hasText(doc.getIfsPath())) {
            storageService.restoreIfsQuietly(doc.getIfsPath());
        }
        doc.setDeleted(0);
        doc.setDeletedTime(null);
        doc.setUpdatedTime(LocalDateTime.now());
        docMapper.updateById(doc);
    }

    public void purge(Long id) {
        Doc doc = docMapper.selectById(id);
        if (doc == null || doc.getDeleted() == null || doc.getDeleted() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "已删除文档不存在");
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

    /* ---------------- 文件（委托 DocStorageService） ---------------- */

    public Doc detail(Long id) {
        return requireDoc(id);
    }

    public DocFileVO file(Long id) {
        Doc doc = requireDoc(id);
        return storageService.readFile(id, doc.getIfsPath());
    }

    /* ---------------- 内部辅助 ---------------- */

    private Doc requireDoc(Long id) {
        Doc doc = docMapper.selectById(id);
        if (doc == null || (doc.getDeleted() != null && doc.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        return doc;
    }
}
