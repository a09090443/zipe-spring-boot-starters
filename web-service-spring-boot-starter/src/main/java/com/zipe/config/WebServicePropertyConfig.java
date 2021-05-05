package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "web.service")
@Data
public class WebServicePropertyConfig {

    private String uriMapping;

    private Map<String, Service> map;
}
