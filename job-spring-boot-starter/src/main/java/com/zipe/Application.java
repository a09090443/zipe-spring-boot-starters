package com.zipe;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

/**
 * job-spring-boot-starter 的 Spring Boot 應用程式進入點。
 * <p>
 * 負責啟動整個排程服務應用，並透過 {@link SpringApplicationBuilder}
 * 設定應用程式脈絡（ApplicationContext）初始化行為，
 * 明確禁止 Bean 定義覆蓋，以提早發現重複 Bean 的衝突問題。
 * </p>
 */
@SpringBootApplication
public class Application {

    /**
     * 應用程式主進入點。
     * <p>
     * 以 {@link SpringApplicationBuilder} 建構並啟動 Spring 應用，
     * 並設定 {@code allowBeanDefinitionOverriding = false}，
     * 防止不同來源的 Bean 定義互相覆蓋，強化啟動時期的問題偵測。
     * </p>
     *
     * @param args 命令列啟動參數
     */
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                // 禁止 Bean 定義覆蓋，避免多個配置來源產生衝突卻無聲略過
                .initializers((ApplicationContextInitializer<GenericApplicationContext>) applicationContext -> applicationContext.setAllowBeanDefinitionOverriding(false))
                .run(args);
    }

}
