package com.zipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * web-service-spring-boot-starter 的 Spring Boot 應用程式進入點。
 * <p>
 * 透過 {@link SpringBootApplication} 啟用自動配置與元件掃描，
 * 使 CXF WebService 相關的自動配置類別得以載入並完成服務端與客戶端的初始化。
 * </p>
 */
@SpringBootApplication
public class Application {

    /**
     * 應用程式主進入點，啟動 Spring Boot 容器。
     *
     * @param args 命令列啟動參數
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
