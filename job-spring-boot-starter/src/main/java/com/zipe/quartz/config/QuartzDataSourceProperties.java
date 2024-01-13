package com.zipe.quartz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.datasource.quartz")
public class QuartzDataSourceProperties {
    public String username;
    public String password;
    public String url;
    public String driverClassName;
}
