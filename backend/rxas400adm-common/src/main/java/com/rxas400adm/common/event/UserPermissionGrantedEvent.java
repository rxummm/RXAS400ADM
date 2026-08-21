package com.rxas400adm.common.event;

/**
 * 权限自助申请审批通过事件（跨模块发布）：system 模块完成角色/权限绑定后发布，
 * security 模块监听并刷新该用户权限缓存（PermissionService.evict）。
 */
public record UserPermissionGrantedEvent(String username) {
}