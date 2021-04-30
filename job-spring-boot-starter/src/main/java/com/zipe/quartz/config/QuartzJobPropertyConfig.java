package com.zipe.quartz.config;

import com.zipe.quartz.model.Job;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/28 下午 03:42
 **/
@Configuration
@ConfigurationProperties(prefix = "quartz")
@Data
public class QuartzJobPropertyConfig {
    private Map<String, Job> jobMap;
}
