package com.rxas400adm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * P2: 全局 OpenAPI 配置，Swagger UI 自动生成 API 文档。
 * 添加 @Tag 到 Controller 层可分组显示。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RXAS400ADM — IBM i 运维管理平台 API")
                        .version("1.0.0")
                        .description("Spring Boot 3.3 + Vue 3 前后端分离运维管理平台 API 文档")
                        .contact(new Contact().name("RXAS400ADM Team")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Token（登录后获取）")));
    }
}
