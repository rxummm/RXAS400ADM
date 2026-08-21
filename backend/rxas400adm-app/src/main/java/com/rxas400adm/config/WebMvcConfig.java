package com.rxas400adm.config;

import com.rxas400adm.as400.interceptor.As400ServerIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 AS400 服务器选择拦截器：
 * 从 X-AS400-Server Header 提取当前操作的服务器，注入 ThreadLocal。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final As400ServerIdInterceptor as400ServerIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(as400ServerIdInterceptor)
                .addPathPatterns("/api/**");
    }
}
