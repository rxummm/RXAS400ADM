package com.rxas400adm.config;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rxas400adm.config.service.PlatformTaskService;
import com.rxas400adm.config.vo.TaskInfoVO;
import com.rxas400adm.config.vo.TaskTriggerVO;

import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 平台定时任务视图（Spring @Scheduled 自省）：
 * 扫描容器内带 @Scheduled 方法的 Bean，展示触发配置，支持手动触发一次。
 * 权限码：SYS_TASK_MANAGE。
 * 业务编排（白名单/限频/Bean 解析/反射校验/异步执行）见 {@link PlatformTaskService}。
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "定时任务")
public class PlatformTaskController {

    private final PlatformTaskService platformTaskService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYS_TASK_MANAGE')")
    public ApiResponse<List<TaskInfoVO>> tasks() {
        return ApiResponse.success(platformTaskService.listTasks());
    }

    /** 手动触发一次（异步执行，立即返回） */
    @PostMapping("/{beanName}/{methodName}/trigger")
    @PreAuthorize("hasAuthority('SYS_TASK_MANAGE')")
    @OperateLog(module = "定时任务", operation = "手动触发任务")
    public ApiResponse<TaskTriggerVO> trigger(@PathVariable String beanName,
                                              @PathVariable String methodName) {
        LocalDateTime triggeredAt = platformTaskService.trigger(beanName, methodName);
        return ApiResponse.success(new TaskTriggerVO(true, triggeredAt));
    }
}
