package com.zipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * logon-spring-boot-starter 的 Spring Boot 應用程式進入點。
 *
 * <p>繼承 {@link SpringBootServletInitializer} 以同時支援嵌入式容器啟動
 * 與傳統 WAR 部署至外部 Servlet 容器（如 Tomcat）。
 *
 * @author Zipe
 */
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    /**
     * 應用程式主進入點，以嵌入式 Servlet 容器方式啟動 Spring Boot。
     *
     * @param args 命令列引數
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 提供 WAR 部署時的 Spring Boot 應用程式設定。
     *
     * <p>當應用程式以 WAR 形式部署至外部 Servlet 容器時，
     * 容器會透過此方法取得 {@link SpringApplicationBuilder} 設定。
     *
     * @param application 由容器傳入的 {@link SpringApplicationBuilder} 實例
     * @return 已設定應用程式來源的 {@link SpringApplicationBuilder}
     */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}

}
