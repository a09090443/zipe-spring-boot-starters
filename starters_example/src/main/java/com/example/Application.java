package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 應用程式進入點。
 *
 * <p>此類別為 Spring Boot 應用程式的啟動入口，透過 {@link SpringBootApplication} 啟用
 * 元件掃描、自動配置與 Spring Boot 全部預設功能，整合所有 zipe-spring-boot-starters
 * 模組的範例情境。</p>
 *
 * @author Gary.tsai
 */
@SpringBootApplication
public class Application {

    /**
     * 啟動 Spring Boot 應用程式。
     *
     * @param args 命令列引數，會由 Spring Boot 解析並對應至 {@code ApplicationArguments}
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
