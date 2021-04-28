package com.zipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@PropertySource(value = {"classpath:spring-quartz.properties", "classpath:quartz-datasource.properties", "classpath:hikari-config.properties", "classpath:quartz-jobs.properties"}, encoding = "UTF-8")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
