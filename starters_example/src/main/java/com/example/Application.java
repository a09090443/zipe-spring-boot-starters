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
 * <p>本應用自身的 JPA Repository（{@code com.example.repository}）改由 db-starter 的
 * {@code dynamic.base-packages} 設定（見 {@code data-source.properties}）驅動掃描，無須在此
 * 宣告 {@code @EnableJpaRepositories}。因 iam-starter 自帶 {@code @EnableJpaRepositories}
 * 會使 Spring Boot 對應用主套件的 Repository 自動掃描退讓，故以該設定重新啟用，與 iam 的
 * {@code com.zipe.repository} 各自獨立掃描、互不影響。</p>
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
