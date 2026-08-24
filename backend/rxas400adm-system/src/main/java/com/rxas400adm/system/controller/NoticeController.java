package com.rxas400adm.system.controller;
import com.rxas400adm.common.util.SecurityUtils;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.NoticeDTO;
import com.rxas400adm.system.service.INoticeService;
import com.rxas400adm.system.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 系统公告（rx_notice）：已发布公告对登录用户开放；管理端 CRUD 需 NOTICE_MANAGE。
 */
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@Tag(name = "系统公告")
public class NoticeController {

    private final INoticeService noticeService;

    /** 已发布公告（通知中心/首页展示，登录即可读） */
    @GetMapping
    public ApiResponse<List<NoticeVO>> published() {
        return ApiResponse.success(noticeService.published().stream().map(NoticeVO::from).toList());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public ApiResponse<PageResult<NoticeVO>> page(@RequestParam(defaultValue = "1") int current,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        return ApiResponse.success(noticeService.page(current, size, keyword, status).map(NoticeVO::from));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    @OperateLog(module = "通知公告", operation = "发布公告")
    public ApiResponse<NoticeVO> create(@Valid @RequestBody NoticeDTO notice) {
        return ApiResponse.success(NoticeVO.from(noticeService.create(notice, SecurityUtils.currentUsername())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    @OperateLog(module = "通知公告", operation = "修改公告")
    public ApiResponse<NoticeVO> update(@PathVariable Long id, @Valid @RequestBody NoticeDTO dto) {
        return ApiResponse.success(NoticeVO.from(noticeService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    @OperateLog(module = "通知公告", operation = "删除公告")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.success(null);
    }
}
