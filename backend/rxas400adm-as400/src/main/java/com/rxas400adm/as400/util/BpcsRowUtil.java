package com.rxas400adm.as400.util;

import com.rxas400adm.common.util.BpcsDateUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

/**
 * BPCS row-level utility methods shared across all BPCS Service implementations.
 * Eliminates duplicate private static helpers that were copy-pasted in 7+ services.
 */
@Slf4j
public final class BpcsRowUtil {

    private BpcsRowUtil() {}

    /**
     * Case-insensitive string equality check on a map row.
     */
    public static boolean strEq(Map<String, Object> row, String key, String val) {
        if (row == null || key == null || val == null) {
            return false;
        }
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)) {
                String v = en.getValue() == null ? "" : String.valueOf(en.getValue()).trim();
                return v.equalsIgnoreCase(val.trim());
            }
        }
        return false;
    }

    /**
     * Case-insensitive contains check on a map row.
     */
    public static boolean strContains(Map<String, Object> row, String key, String val) {
        if (row == null || key == null || val == null) {
            return false;
        }
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)) {
                String v = en.getValue() == null ? "" : String.valueOf(en.getValue()).trim();
                return v.toLowerCase().contains(val.trim().toLowerCase());
            }
        }
        return false;
    }

    /**
     * Get a trimmed string value from a map row, or null if missing/empty.
     */
    public static String pickStr(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)
                    && en.getValue() != null
                    && !String.valueOf(en.getValue()).trim().isEmpty()) {
                return String.valueOf(en.getValue()).trim();
            }
        }
        return null;
    }

    /**
     * Varargs pickStr: try keys in order, return first non-null result.
     */
    public static String pickStr(Map<String, Object> row, String... keys) {
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            String result = pickStr(row, key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Get an int value from a map row, or 0 if missing/not a number.
     */
    public static int intVal(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return 0;
        }
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)
                    && en.getValue() instanceof Number n) {
                return n.intValue();
            }
        }
        return 0;
    }

    /**
     * Get an Integer value from a map row, or null if missing/not a number.
     */
    public static Integer intOrNull(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)
                    && en.getValue() instanceof Number n) {
                return n.intValue();
            }
        }
        return null;
    }

    /**
     * Varargs intOrNull: try keys in order, return first non-null result.
     */
    public static Integer intOrNull(Map<String, Object> row, String... keys) {
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            Integer result = intOrNull(row, key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Get a BigDecimal value from a map row, or null if missing/not a number.
     */
    public static BigDecimal decOrNull(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)
                    && en.getValue() instanceof Number n) {
                return BigDecimal.valueOf(n.doubleValue());
            }
        }
        return null;
    }

    /**
     * Varargs decOrNull: try keys in order, return first non-null result.
     */
    public static BigDecimal decOrNull(Map<String, Object> row, String... keys) {
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            BigDecimal result = decOrNull(row, key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Get a Double value from a map row, or null if missing/not a number.
     */
    public static Double dblOrNull(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)
                    && en.getValue() instanceof Number n) {
                return n.doubleValue();
            }
        }
        return null;
    }

    /**
     * Convert a numeric CYYMMDD/YYYMMDD field to an ISO date string (yyyy-MM-dd).
     * Returns null if conversion fails.
     */
    public static String dateStr(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)
                    && en.getValue() instanceof Number n) {
                try {
                    String s = String.valueOf(n.intValue());
                    if (s.length() == 8 || s.length() == 7) {
                        return BpcsDateUtil.toLocalDate(s).toString();
                    }
                } catch (Exception e) {
                    log.debug("Date parse failed for key={}: {}", key, e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Varargs dateStr: try keys in order, return first non-null result.
     */
    public static String dateStr(Map<String, Object> row, String... keys) {
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            String result = dateStr(row, key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
