package com.rxas400adm.operation.annotation;

import com.rxas400adm.operation.domain.RiskLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IbmiOperation {
    String code();
    RiskLevel riskLevel();
    String requiredPermission() default "";
    String description() default "";
}