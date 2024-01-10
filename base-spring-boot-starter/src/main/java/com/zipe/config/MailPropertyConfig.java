package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 **/
@Configuration
@ConfigurationProperties(prefix = "mail")
@Data
public class MailPropertyConfig {
    /**
     * Mail debug mode 開罐
     * Default=false
     */
    private Boolean debugEnable = false;
    /**
     * Smtp 帳號
     */
    private String username;
    /**
     * Smtp 密碼
     */
    private String pa55word;
    /**
     * 加密開關
     * Default=false
     */
    private Boolean encryptEnable = false;
    /**
     * Mail server
     */
    private String host;
    /**
     * Mail server port
     */
    private String port;
    /**
     * Smtp 認證開關
     * Default=true
     */
    private Boolean smtpAuthEnable = true;
    /**
     * 寄送人
     */
    private String sender;
    /**
     * Mail server protocols
     * Default=smtp
     */
    private String transportProtocol = "smtp";
    /**
     * Smtp tls 開關
     * Default=false
     */
    private Boolean smtpStartTlsEnable = false;
}
