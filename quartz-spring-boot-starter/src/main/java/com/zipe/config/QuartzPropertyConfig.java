package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zipe
 */
@Configuration
@ConfigurationProperties(prefix = "quartz")
@Data
public class QuartzPropertyConfig {
    private String jobName;
    private String jobType;
}
