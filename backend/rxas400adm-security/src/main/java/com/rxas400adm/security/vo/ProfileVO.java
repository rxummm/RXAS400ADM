package com.rxas400adm.security.vo;

import java.util.List;

/**
 * 用户信息响应 VO（替代 Map&lt;String, Object&gt;）。
 */
public record ProfileVO(String username, String email, List<String> permissions) {}
