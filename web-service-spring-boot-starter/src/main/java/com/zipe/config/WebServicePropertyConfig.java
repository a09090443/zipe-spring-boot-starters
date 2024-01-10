package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author Gary.Tsai
 */
@Configuration
@ConfigurationProperties(prefix = "web.service")
@Data
public class WebServicePropertyConfig {
    /**
     * 設定 Uri
     * Default=/webservice/*
     */
    private String uriMapping = "/webservice/*";
    /**
     * 設定對應的 name 和 class
     */
    private Map<String, Service> map;
}
