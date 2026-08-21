package com.rxas400adm.security;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @WebMvcTest 切片配置锚点：屏蔽主启动类 {@code Rxas400admApplication} 的
 * {@code @MapperScan}（避免切片上下文创建真实 MyBatis Mapper Bean），
 * 保留与主应用一致的 {@code @SpringBootApplication} 扫描范围与 TypeExcludeFilter 机制，
 * 使 {@code @WebMvcTest(controllers=...)} 能按属性注册指定控制器。
 */
@SpringBootApplication(scanBasePackages = "com.rxas400adm")
public class TestSecuritySliceConfig {
}
