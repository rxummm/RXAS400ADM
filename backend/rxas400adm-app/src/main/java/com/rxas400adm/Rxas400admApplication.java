package com.rxas400adm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RXAS400 - Enterprise IBM i Operation Platform
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan({"com.rxas400adm.**.mapper", "com.rxas400adm.**.collaboration", "com.rxas400adm.**.freight", "com.rxas400adm.**.simulation"})
public class Rxas400admApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rxas400admApplication.class, args);
    }
}