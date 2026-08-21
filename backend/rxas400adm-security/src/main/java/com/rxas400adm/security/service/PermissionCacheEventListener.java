package com.rxas400adm.security.service;

import com.rxas400adm.common.event.UserPermissionGrantedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 权限缓存刷新监听：自助权限申请审批通过后（system 模块发布事件），
 * 使该用户权限缓存立即失效，下次加载即为最新权限。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionCacheEventListener {

    private final IPermissionService permissionService;

    @Async
    @EventListener
    public void onPermissionGranted(UserPermissionGrantedEvent event) {
        permissionService.evict(event.username());
        log.info("权限变更事件，已失效权限缓存: {}", event.username());
    }
}