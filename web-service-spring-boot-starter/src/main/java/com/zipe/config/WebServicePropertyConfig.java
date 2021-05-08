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

    private String uriMapping = "/webservice/*";

    private Map<String, Service> map;
}
