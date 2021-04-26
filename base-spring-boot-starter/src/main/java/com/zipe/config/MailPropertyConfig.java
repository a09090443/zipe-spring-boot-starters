package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/26 下午 01:37
 **/
@Configuration
@ConfigurationProperties(prefix = "mail")
@Data
public class MailPropertyConfig {
    private Boolean debugEnable = false;
    private String username;
    private String pa55word;
    private Boolean encryptEnable = false;
    private String host;
    private String port;
    private Boolean smtpAuthEnable = true;
    private String sender;
    private String transportProtocol = "smtp";
    private Boolean smtpStartTlsEnable = false;
}
