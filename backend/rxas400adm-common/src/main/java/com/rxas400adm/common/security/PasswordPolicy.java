package com.rxas400adm.common.security;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 统一密码强度策略（P2-3）：改密/建户/重置密码共用同一套校验，消除各处不一致。
 * 规则：至少 8 位，且同时包含字母与数字。
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static final int MIN_LENGTH = 8;

    private static final Pattern HAS_LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");

    /** 校验密码强度，不通过抛 BusinessException（带友好提示） */
    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "密码长度不能少于 " + MIN_LENGTH + " 位");
        }
        if (!HAS_LETTER.matcher(password).find() || !HAS_DIGIT.matcher(password).find()) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "密码必须同时包含字母和数字");
        }
    }

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    /** 生成 12 位强随机密码（无 0/O/1/l 易混淆字符），用于创建用户未指定密码时兜底 */
    public static String generateRandom() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
