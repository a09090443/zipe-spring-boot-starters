package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;

/**
 * Keycloak 範例應用程式的啟動入口。
 *
 * <p>使用 {@link SpringBootApplication} 啟動 Spring Boot 應用，
 * 並排除 {@link LiquibaseAutoConfiguration}，
 * 以避免範例環境中因缺少 Liquibase 設定而導致啟動失敗。
 */
@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
public class Application {

    /**
     * 應用程式主要進入點，負責啟動 Spring Boot 容器。
     *
     * @param args 命令列參數
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
