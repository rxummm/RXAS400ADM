package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.MessageFileRow;
import com.rxas400adm.as400.model.MessageRow;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * 消息文件（*MSGF）管理：按 库 + 消息文件名 查询消息文件（QSYS2.MESSAGE_FILE_INFO）
 * 与文件内消息描述（QSYS2.MESSAGE_DESCRIPTION_INFO：MESSAGE_ID / 文本 / 二级文本 / 严重级别），
 * 支持消息描述增删改查（ADDMSGD / CHGMSGD / RMVMSGD），按钮级授权 MSGF_ADD / MSGF_EDIT / MSGF_DELETE。
 * 数据源按 X-AS400-Server 头路由当前服务器。
 */
@RestController
@RequestMapping("/api/v1/message-files")
@RequiredArgsConstructor
@Tag(name = "消息文件")
public class MessageFileController {

    private final AS400ClientProvider clientProvider;

    /** 消息文件列表（按库过滤） */
    @GetMapping("/files")
    @PreAuthorize("hasAuthority('MSGF_VIEW')")
    public ApiResponse<List<MessageFileRow>> files(@RequestParam(required = false) String library) {
        return ApiResponse.success(clientProvider.current().listMessageFiles(library));
    }

    /** 文件内消息描述列表（MESSAGE_ID / 文本 / 二级文本 / 严重级别，关键词模糊过滤） */
    @GetMapping("/messages")
    @PreAuthorize("hasAuthority('MSGF_VIEW')")
    public ApiResponse<List<MessageRow>> messages(@RequestParam(required = false) String library,
                                                  @RequestParam String file,
                                                  @RequestParam(required = false) String keyword) {
        return ApiResponse.success(clientProvider.current().listMessages(library, file, keyword));
    }

    /** 新增消息描述（ADDMSGD） */
    @PostMapping("/messages")
    @PreAuthorize("hasAuthority('MSGF_ADD')")
    @OperateLog(module = "消息文件", operation = "新增消息描述")
    public ApiResponse<CommandResult> add(@Valid @RequestBody MessageRequest request) {
        return ApiResponse.success(clientProvider.current().addMessage(
                request.getLibrary(), request.getFile(), request.getId(),
                request.getText(), request.getSecondLevel(), request.getSeverity()));
    }

    /** 修改消息描述（CHGMSGD） */
    @PutMapping("/messages")
    @PreAuthorize("hasAuthority('MSGF_EDIT')")
    @OperateLog(module = "消息文件", operation = "修改消息描述")
    public ApiResponse<CommandResult> update(@Valid @RequestBody MessageRequest request) {
        return ApiResponse.success(clientProvider.current().updateMessage(
                request.getLibrary(), request.getFile(), request.getId(),
                request.getText(), request.getSecondLevel(), request.getSeverity()));
    }

    /** 删除消息描述（RMVMSGD） */
    @DeleteMapping("/messages")
    @PreAuthorize("hasAuthority('MSGF_DELETE')")
    @OperateLog(module = "消息文件", operation = "删除消息描述")
    public ApiResponse<CommandResult> delete(@RequestParam(required = false) String library,
                                             @RequestParam String file,
                                             @RequestParam String id) {
        return ApiResponse.success(clientProvider.current().deleteMessage(library, file, id));
    }

    /** 消息描述请求体 */
    @Data
    public static class MessageRequest {
        private String library;
        private String file;
        private String id;
        private String text;
        private String secondLevel;
        private int severity;
    }
}
