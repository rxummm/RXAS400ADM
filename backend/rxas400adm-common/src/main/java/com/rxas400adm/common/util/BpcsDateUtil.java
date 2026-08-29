package com.rxas400adm.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 【AS400 业务增强·P1】BPCS 日期工具。
 *
 * <p>支持两种 IBM i / BPCS 常见日期格式：
 * <ul>
 *   <li>YYYYMMDD（8 位数字，如 20250312）—— 多数现代环境</li>
 *   <li>CYYMMDD（7 位数字，世纪位 0/1 + 年月日，如 1250312 = 2025-03-12）—— 旧版 BPCS/V5R4 环境</li>
 * </ul>
 *
 * <p>空值/非法值一律返回 null（展示层显示 "—"），不向上抛异常——ERP 存量数据中存在脏日期是常态。
 */
public final class BpcsDateUtil {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private BpcsDateUtil() {
    }

    /**
     * 日期数字 → LocalDate。
     * <p>自动识别格式：8 位按 YYYYMMDD 解析；7 位按 CYYMMDD 解析（C=世纪位，0=19xx，1=20xx）。
     * 其他长度或非法值返回 null。
     */
    public static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        // 去除可能的小数尾（数据库 BigDecimal 序列化为 "20250312.0"）
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        try {
            int n = Integer.parseInt(s);
            if (n <= 0) {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (s.length() == 8) {
            return parseYYYYMMDD(s);
        }
        if (s.length() == 7) {
            return parseCYYMMDD(s);
        }
        return null;
    }

    /** YYYYMMDD 解析 */
    private static LocalDate parseYYYYMMDD(String s) {
        try {
            return LocalDate.parse(s, YYYYMMDD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * CYYMMDD 解析（C=世纪位：0→19xx，1→20xx，2→21xx…）。
     * <p>示例：1250312 → 2025-03-12；0991231 → 1999-12-31。
     */
    private static LocalDate parseCYYMMDD(String s) {
        try {
            int century = Character.getNumericValue(s.charAt(0));
            int year = Integer.parseInt(s.substring(1, 3));
            int month = Integer.parseInt(s.substring(3, 5));
            int day = Integer.parseInt(s.substring(5, 7));
            int fullYear = 1900 + century * 100 + year;
            return LocalDate.of(fullYear, month, day);
        } catch (Exception e) {
            return null;
        }
    }

}
