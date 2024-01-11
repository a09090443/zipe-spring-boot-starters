package com.zipe.quartz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.zipe.quartz.model.Job;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "quartz")
@Data
public class QuartzJobPropertyConfig {
    private Map<String, Job> jobMap = new HashMap<>();
}
