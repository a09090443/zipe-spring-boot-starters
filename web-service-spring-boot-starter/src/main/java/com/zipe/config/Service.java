package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Gary Tsai
 */
@Configuration
@ConfigurationProperties(prefix="web.service.map")
@Data
public class Service {
    /**
     * Spring bean name
     */
    private String beanName;
    /**
     * Uri namespace
     */
    private String uriMapping;
}
