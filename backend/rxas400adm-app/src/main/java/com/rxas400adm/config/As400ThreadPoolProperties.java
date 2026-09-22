package com.rxas400adm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MSGW 线程池配置参数，从 application.yml 的 rxas400.threadpool.msgw 读取。
 */
@Data
@ConfigurationProperties(prefix = "rxas400.threadpool.msgw")
public class As400ThreadPoolProperties {

    private int corePoolSize = 4;
    private int maxPoolSize = 8;
    private int queueCapacity = 100;
    private long keepAliveSeconds = 60;
}
