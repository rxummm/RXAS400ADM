package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.DocDTO;
import com.rxas400adm.as400.dto.DocTemplateDTO;
import com.rxas400adm.as400.entity.Doc;
import com.rxas400adm.as400.service.DocService;
import com.rxas400adm.as400.service.DocVersionService;
import com.rxas400adm.as400.vo.DocFileVO;
import com.rxas400adm.as400.vo.DocTemplateVO;
import com.rxas400adm.as400.vo.DocVersionVO;
import com.rxas400adm.as400.vo.DocVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档管理（3.9）：模板 / 文档 CRUD + 审批流（提交/通过/驳回）+ 版本历史。
 * 查看 DOC_VIEW、编辑 DOC_MANAGE、审批 DOC_APPROVE。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "文档模板 / 文档 CRUD / 审批流 / 版本管理")
public class DocController {

    private final DocService docService;
    private final DocVersionService docVersionService;

    /* ---------------- 模板 ---------------- */

    @GetMapping("/doc-templates")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ApiResponse<List<DocTemplateVO>> templates(@RequestParam(required = false) String category) {
        return ApiResponse.success(docService.listTemplates(category));
    }

    @PostMapping("/doc-templates")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "新建文档模板")
    public ApiResponse<DocTemplateVO> createTemplate(@Valid @RequestBody DocTemplateDTO template) {
        return ApiResponse.success(DocTemplateVO.from(docService.createTemplate(template, currentUser())));
    }

    @PutMapping("/doc-templates/{id}")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "编辑文档模板")
    public ApiResponse<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody DocTemplateDTO template) {
        docService.updateTemplate(id, template, currentUser());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/doc-templates/{id}")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "删除文档模板")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        docService.deleteTemplate(id);
        return ApiResponse.success(null);
    }

    /* ---------------- 文档 ---------------- */

    @GetMapping("/docs")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ApiResponse<PageResult<DocVO>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "0") int deleted,
                                                 @RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "20") long size) {
        PageResult<Doc> page = docService.listDocs(keyword, status, current, size, deleted == 1);
        return ApiResponse.success(new PageResult<>(page.getTotal(), page.getRecords().stream().map(DocVO::from).toList()));
    }

    @GetMapping("/docs/{id}")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ApiResponse<DocVO> detail(@PathVariable Long id) {
        return ApiResponse.success(DocVO.from(docService.detail(id)));
    }

    @PostMapping("/docs")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "新建文档")
    public ApiResponse<DocVO> create(@Valid @RequestBody DocDTO doc) {
        return ApiResponse.success(DocVO.from(docService.createDoc(doc, currentUser())));
    }

    @PutMapping("/docs/{id}")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "编辑文档")
    public ApiResponse<DocVO> update(@PathVariable Long id, @Valid @RequestBody DocDTO doc) {
        return ApiResponse.success(DocVO.from(docService.updateDoc(id, doc, currentUser())));
    }

    @DeleteMapping("/docs/{id}")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "删除文档")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        docService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/docs/{id}/restore")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "恢复已删除文档")
    public ApiResponse<Void> restore(@PathVariable Long id) {
        docService.restore(id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/docs/{id}/purge")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "永久删除文档")
    public ApiResponse<Void> purge(@PathVariable Long id) {
        docService.purge(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/docs/{id}/submit")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "提交审批")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        docService.submit(id, currentUser());
        return ApiResponse.success(null);
    }

    @PostMapping("/docs/{id}/approve")
    @PreAuthorize("hasAuthority('DOC_APPROVE')")
    @OperateLog(module = "文档管理", operation = "审批通过")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        docService.approve(id, currentUser());
        return ApiResponse.success(null);
    }

    @PostMapping("/docs/{id}/reject")
    @PreAuthorize("hasAuthority('DOC_APPROVE')")
    @OperateLog(module = "文档管理", operation = "审批驳回")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestParam String reason) {
        docService.reject(id, reason, currentUser());
        return ApiResponse.success(null);
    }

    @GetMapping("/docs/{id}/versions")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ApiResponse<List<DocVersionVO>> versions(@PathVariable Long id) {
        return ApiResponse.success(docService.versions(id));
    }

    @GetMapping("/docs/versions/{versionId}/content")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ApiResponse<Map<String, String>> versionContent(@PathVariable Long versionId) {
        String content = docVersionService.versionContent(versionId);
        Map<String, String> body = new HashMap<>();
        body.put("content", content);
        return ApiResponse.success(body);
    }

    @PostMapping("/docs/{id}/rollback/{version}")
    @PreAuthorize("hasAuthority('DOC_MANAGE')")
    @OperateLog(module = "文档管理", operation = "版本回滚")
    public ApiResponse<Void> rollback(@PathVariable Long id, @PathVariable Integer version) {
        docService.rollback(id, version, currentUser());
        return ApiResponse.success(null);
    }

    @GetMapping("/docs/{id}/file")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<byte[]> file(@PathVariable Long id) {
        DocFileVO file = docService.file(id);
        if (file.bytes() == null || file.bytes().length == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "IFS 文件内容为空");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    private String currentUser() {
        return SecurityUtils.currentUsername();
    }
}
