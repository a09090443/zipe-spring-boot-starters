package com.zipe.config;

import java.util.ArrayList;
import java.util.List;
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
    /**
     * 多組 SMTP 伺服器設定（優先序 failover，索引即嘗試順序）。
     * 未設定（空清單）時，以上方扁平欄位（host/port/username/pa55word/...）合成單一組，
     * 維持升級前僅設定單組 SMTP 的既有行為。
     */
    private List<MailServerProperty> servers = new ArrayList<>();
    /**
     * SMTP 連線逾時（毫秒），套用至所有伺服器組。
     * Default=5000
     */
    private Integer connectionTimeout = 5000;
    /**
     * SMTP 讀取逾時（毫秒），套用至所有伺服器組。
     * Default=3000
     */
    private Integer readTimeout = 3000;
    /**
     * SMTP 寫入逾時（毫秒），套用至所有伺服器組。
     * Default=5000
     */
    private Integer writeTimeout = 5000;
    /**
     * 多 SMTP 容錯切換行為設定（最大嘗試組數、整體切換時間上限）。
     */
    private MailFailoverProperty failover = new MailFailoverProperty();

    /**
     * 取得供容錯切換使用的伺服器清單。
     * <p>
     * 若 {@link #servers} 已設定則直接回傳（清單順序即優先序）；若未設定（空清單），
     * 則由既有扁平欄位（{@link #host}、{@link #port} 等）合成唯一一組，確保僅設定
     * 舊版單組 {@code mail.*} 屬性的使用方升級後行為不變。
     * </p>
     *
     * @return 依優先序排列的 SMTP 伺服器設定清單
     */
    public List<MailServerProperty> resolveServers() {
        if (servers != null && !servers.isEmpty()) {
            return servers;
        }
        MailServerProperty legacy = new MailServerProperty();
        legacy.setName("default");
        legacy.setHost(host);
        legacy.setPort(port);
        legacy.setUsername(username);
        legacy.setPa55word(pa55word);
        legacy.setEncryptEnable(encryptEnable);
        legacy.setSmtpAuthEnable(smtpAuthEnable);
        legacy.setSmtpStartTlsEnable(smtpStartTlsEnable);
        legacy.setTransportProtocol(transportProtocol);
        return List.of(legacy);
    }
}
