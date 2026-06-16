package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 應用程式進入點。
 *
 * <p>此類別為 Spring Boot 應用程式的啟動入口，透過 {@link SpringBootApplication} 啟用
 * 元件掃描、自動配置與 Spring Boot 全部預設功能，整合所有 zipe-spring-boot-starters
 * 模組的範例情境。</p>
 *
 * <p>顯式宣告 {@link EnableJpaRepositories} 掃描自身的 {@code com.example.repository}：
 * 因 iam-starter 帶有自己的 {@code @EnableJpaRepositories}，會使 Spring Boot 對應用主套件的
 * JPA Repository 自動掃描退讓；故本應用須自行宣告，與 iam 的 {@code com.zipe.repository}
 * 各自獨立掃描、互不影響。</p>
 *
 * @author Gary.tsai
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.repository")
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
