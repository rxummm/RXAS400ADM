package com.rxas400adm.email.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.email.dto.EmailGroupCreateDTO;
import com.rxas400adm.email.dto.EmailRecipientDTO;
import com.rxas400adm.email.service.IEmailGroupService;
import com.rxas400adm.email.vo.EmailGroupVO;
import com.rxas400adm.email.vo.EmailRecipientVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;


/**
 * 收件人分组管理：CRUD + 成员管理。
 */
@RestController
@RequestMapping("/api/v1/email/groups")
@RequiredArgsConstructor
@Tag(name = "Email Groups", description = "Recipient group management")
public class EmailGroupController {

    private final IEmailGroupService emailGroupService;

    @GetMapping
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    public ApiResponse<PageResult<EmailGroupVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(emailGroupService.page(current, size, keyword));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('EMAIL_SEND')")
    public ApiResponse<List<EmailGroupVO>> listAll() {
        return ApiResponse.success(emailGroupService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    @OperateLog(module = "邮件管理", operation = "创建收件人分组")
    public ApiResponse<Void> create(@Valid @RequestBody EmailGroupCreateDTO dto) {
        emailGroupService.create(dto);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    @OperateLog(module = "邮件管理", operation = "更新收件人分组")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody EmailGroupCreateDTO dto) {
        emailGroupService.update(id, dto);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    @OperateLog(module = "邮件管理", operation = "删除收件人分组")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        emailGroupService.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    public ApiResponse<List<EmailRecipientVO>> members(@PathVariable Long id) {
        List<EmailRecipientVO> vos = emailGroupService.members(id).stream()
                .map(EmailRecipientVO::from)
                .toList();
        return ApiResponse.success(vos);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    @OperateLog(module = "邮件管理", operation = "添加分组成员")
    public ApiResponse<Void> addMember(@PathVariable Long id, @Valid @RequestBody EmailRecipientDTO dto) {
        emailGroupService.addMember(id, dto);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    @OperateLog(module = "邮件管理", operation = "移除分组成员")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        emailGroupService.removeMember(id, memberId);
        return ApiResponse.success(null);
    }
}