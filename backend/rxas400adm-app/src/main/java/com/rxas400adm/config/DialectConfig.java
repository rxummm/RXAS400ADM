package com.rxas400adm.config;

import com.rxas400adm.common.constants.SqlDialect;
import com.rxas400adm.common.constants.SqlDialectHolder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * H2：启动时按主数据源 URL 注入 SQL 方言——MySQL → {@link SqlDialect#MYSQL}（dev），
 * {@code jdbc:as400://...} → {@link SqlDialect#DB2_I}（生产部署 DB2 for i）。
 * 供 {@code PageConstants.limitClause} 生成方言化 LIMIT 片段。
 */
@Configuration
public class DialectConfig {

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @PostConstruct
    void init() {
        boolean db2 = jdbcUrl != null && jdbcUrl.startsWith("jdbc:as400");
        SqlDialectHolder.set(db2 ? SqlDialect.DB2_I : SqlDialect.MYSQL);
    }
}