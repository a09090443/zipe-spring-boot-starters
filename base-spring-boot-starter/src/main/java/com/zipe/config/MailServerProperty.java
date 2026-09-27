package com.zipe.config;

import lombok.Data;

/**
 * 單組 SMTP 伺服器設定，作為 {@link MailPropertyConfig#getServers()} 清單中的一個元素。
 *
 * <p>清單中的索引順序即為多 SMTP 容錯切換（failover）的嘗試優先序：由索引 0 開始依序嘗試，
 * 直到某一組成功送出為止。各組欄位彼此獨立，不會繼承 {@link MailPropertyConfig} 頂層的扁平欄位值。</p>
 */
@Data
public class MailServerProperty {
    /**
     * 伺服器識別名稱，僅用於日誌辨識（切換與失敗記錄），未設定時以 host:port 顯示。
     */
    private String name;
    /**
     * Smtp 帳號
     */
    private String username;
    /**
     * Smtp 密碼
     */
    private String pa55word;
    /**
     * 加密開關（true 時 pa55word 以 Base64 解碼後使用）
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
