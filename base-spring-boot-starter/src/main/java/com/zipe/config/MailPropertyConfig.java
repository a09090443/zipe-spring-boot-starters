package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 郵件設定屬性配置類別。
 *
 * <p>對應 {@code application.yml} / {@code application.properties} 中以 {@code mail.} 為前綴的屬性，
 * 集中管理 SMTP 伺服器連線、認證、加密及寄件人等郵件相關設定。
 * 由 Spring Boot 的 {@link org.springframework.boot.context.properties.ConfigurationProperties}
 * 機制自動繫結，並透過 {@link com.zipe.autoconfiguration.BaseAutoConfiguration} 匯入 Spring 容器。</p>
 *
 * @author : Gary Tsai
 **/
@Configuration
@ConfigurationProperties(prefix = "mail")
@Data
public class MailPropertyConfig {
    /**
     * Mail debug mode 開關
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
