package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 **/
@Configuration
@ConfigurationProperties(prefix = "velocity")
@Data
public class VelocityPropertyConfig {
    /**
     * Velocity 目錄
     * Default=template
     */
    private String dirPath = "template";
}
