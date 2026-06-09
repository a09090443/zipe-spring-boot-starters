package com.zipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * web-spring-boot-starter 的應用程式進入點。
 *
 * <p>標註 {@link SpringBootApplication}，啟動 Spring Boot 自動配置、
 * 元件掃描與內嵌 Tomcat 容器，供本模組的整合測試與範例使用。
 */
@SpringBootApplication
public class Application {

    /**
     * 應用程式主方法，啟動 Spring Boot 應用程式。
     *
     * @param args 命令列參數，會轉交給 {@link SpringApplication#run} 處理
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
