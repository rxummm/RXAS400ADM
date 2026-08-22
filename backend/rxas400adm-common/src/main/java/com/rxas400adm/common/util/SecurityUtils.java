package com.rxas400adm.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具（B11：统一 currentUsername 兜底值，消除各 Controller/Service 重复实现）。
 */
public final class SecurityUtils {

    /** 未登录/上下文缺失时的兜底用户名 */
    public static final String ANONYMOUS = "anonymous";

    private SecurityUtils() {
    }

    public static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? ANONYMOUS : authentication.getName();
    }
}
