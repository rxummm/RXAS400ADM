package com.rxas400adm.system.service;

/**
 * 登录审计参数对象（中-7）：auditLogin 六参签名收敛，跨模块（security→system）传递。
 *
 * @param action   动作码（LOGIN_SUCCESS / LOGIN_FAILED / AS400 登录系列 / PASSWORD_CHANGE）
 * @param username 用户名
 * @param ip       来源 IP
 * @param source   来源域（PLATFORM/AS400）
 * @param serverId AS400 服务器 ID（平台登录为 null）
 * @param detail   明细文案
 */
public record LoginAuditContext(String action, String username, String ip, String source,
                                Long serverId, String detail) {
}
