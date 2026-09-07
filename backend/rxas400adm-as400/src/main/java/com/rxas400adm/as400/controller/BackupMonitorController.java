package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBackupMonitorService;
import com.rxas400adm.as400.vo.BackupStatusVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/as400/backup")
@RequiredArgsConstructor
@Tag(name = "备份监控", description = "A4备份状态监控与查询")
public class BackupMonitorController {

    private final IBackupMonitorService backupMonitorService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('BACKUP_VIEW')")
    public ApiResponse<List<BackupStatusVO>> listByServer(@RequestParam(required = false) Long serverId) {
        return ApiResponse.success(backupMonitorService.listByServer(serverId));
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('BACKUP_VIEW')")
    public ApiResponse<BackupStatusVO> getLatestByServer(@RequestParam(required = false) Long serverId) {
        return ApiResponse.success(backupMonitorService.getLatestByServer(serverId));
    }
}
