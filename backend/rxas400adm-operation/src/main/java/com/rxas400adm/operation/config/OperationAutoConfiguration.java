package com.rxas400adm.operation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OperationProperties.class)
@ComponentScan(basePackages = "com.rxas400adm.operation")
public class OperationAutoConfiguration {
}