package com.rxas400adm.as400.util;

/**
 * cron 表达式预校验（P2-13）：
 * 与调度注册使用同一套 Quartz 语义（CronScheduleBuilder），保存时即校验，
 * 避免无效 cron 静默注册失败（任务 enabled 但永不触发）。
 */
public final class CronValidator {

    private CronValidator() {
    }

    /** 校验 Quartz cron 表达式；合法返回 null，非法返回错误信息 */
    public static String validate(String cron) {
        if (cron == null || cron.isBlank()) {
            return "cron 表达式不能为空";
        }
        String expr = cron.trim();
        if (!org.quartz.CronExpression.isValidExpression(expr)) {
            return "无效的 cron 表达式: " + expr;
        }
        return null;
    }
}
