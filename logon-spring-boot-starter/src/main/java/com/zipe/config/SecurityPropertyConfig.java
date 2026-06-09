package com.zipe.config;

import com.zipe.enums.FrameOptionsMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Security 相關屬性設定類別。
 *
 * <p>對應 {@code application.yml} / {@code application.properties} 中以 {@code security} 為前綴的設定項目，
 * 統一管理登入啟用狀態、驗證類型、允許放行路徑、登入頁面、CSRF 防護及 X-Frame-Options 等核心安全參數。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/19 下午 21:15
 **/
@Configuration
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityPropertyConfig {

    /** 是否啟用 Spring Security 安全機制，預設為 {@code true}。 */
    private Boolean enable = Boolean.TRUE;

    /**
     * 驗證類型，對應 {@link com.zipe.enums.VerificationTypeEnum} 中的值（例如 DB / LDAP / Custom）。
     * 若未設定則使用系統預設驗證流程。
     */
    private String verificationType;

    /** 是否啟用登入記錄功能，預設為 {@code false}。 */
    private Boolean recordLogEnable = Boolean.FALSE;

    /**
     * 自訂登入記錄 Bean 的名稱。
     * 當 {@link #recordLogEnable} 為 {@code true} 時，框架將依此名稱從 Spring 容器取得
     * 實作 {@link com.zipe.service.CustomLogonLogRecord} 的 Bean。
     */
    private String customRecordLogBean;

    /**
     * 不需要驗證即可存取的 URI 清單，多筆以逗號分隔（支援 Ant 路徑模式，例如 {@code /public/**,/static/**}）。
     */
    private String allowUris;

    /** 自訂登入頁面的 URI，未設定時使用 Spring Security 預設登入頁。 */
    private String loginUri;

    /** 登入成功後導向的 URI，預設為根路徑 {@code /}。 */
    private String loginSuccessUri = "/";

    /** 登入失敗後導向的 URI，預設為 {@code /error}。 */
    private String loginFailureUri = "/error";

    /**
     * 自訂驗證邏輯 Bean 的名稱。
     * 當 {@link #verificationType} 設為 Custom 時，框架將依此名稱取得對應的驗證服務 Bean。
     */
    private String customBeanName;

    /** 是否啟用 CSRF 防護，預設為 {@code true}。關閉時請確認系統已有其他防護機制。 */
    private Boolean csrfEnabled = Boolean.TRUE;

    /**
     * X-Frame-Options 模式，預設 SAMEORIGIN 以保留點擊劫持防護。
     */
    private FrameOptionsMode frameOptionsMode = FrameOptionsMode.SAMEORIGIN;

    /** LDAP 驗證相關屬性，對應 {@code security.ldap.*} 設定區塊。 */
    private LdapPropertyConfig ldap = new LdapPropertyConfig();
}
