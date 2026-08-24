package com.rxas400adm.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（AOP 自动写入 rx_audit_log）。
 * 用法：@OperateLog(module = "发布中心", operation = "执行发布")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperateLog {

    String module() default "";

    String operation() default "";

    /**
     * 操作对象表达式。
     * 支持 SpEL 表达式，如 "#id" 或 "#request.getParameter('path')"。
     * 示例：@OperateLog(module = "IFS", operation = "删除文件", target = "#path")
     */
    String target() default "";
}
