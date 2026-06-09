package com.zipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;

/**
 * keycloak-spring-boot-starter 的應用程式進入點。
 *
 * <p>啟動嵌入式 Keycloak 身份認證伺服器，並整合 Spring Boot 自動配置機制。
 * 排除 {@link LiquibaseAutoConfiguration}，避免與 Keycloak 內建的資料庫遷移機制產生衝突。
 */
@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
public class Application {

    /**
     * 應用程式主要進入點，啟動 Spring Boot 應用程式。
     *
     * @param args 命令列引數，可傳入 Spring Boot 支援的啟動參數
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
