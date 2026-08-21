package com.rxas400adm.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    /** H2：分页方言随主数据源 URL 切换（MySQL → MYSQL；jdbc:as400 → DB2，DB2 for i 7.1+ 支持 OFFSET/FETCH） */
    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        DbType dbType = jdbcUrl != null && jdbcUrl.startsWith("jdbc:as400") ? DbType.DB2 : DbType.MYSQL;
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
