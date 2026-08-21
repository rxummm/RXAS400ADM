package com.rxas400adm.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 切片测试共享 AOP 配置：启用 @Aspect 代理，让 OperateLogAspect 在
 * {@code @WebMvcTest} 切片中生效。必须为顶层类——嵌套 {@code @Configuration}
 * 会干扰 {@code @WebMvcTest} 的控制器注册（见 404 排查记录）。
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TestAopConfig {
}
