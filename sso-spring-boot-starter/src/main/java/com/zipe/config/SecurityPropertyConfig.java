package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/16 下午 03:37
 **/
@Configuration
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityPropertyConfig {

    private String verificationType;

    private Boolean isRecordLog = false;

    private String[] allowUris = new String[]{"/**"};

    private String loginUri;
}
