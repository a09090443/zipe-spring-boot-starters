package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LDAP 伺服器連線屬性設定類別。
 *
 * <p>對應 {@code application.yml} 中 {@code security.ldap.*} 前綴的設定項目，
 * 供 LDAP 驗證流程取得伺服器位址、網域、連接埠及識別名稱（DN）等連線參數。</p>
 *
 * @author : Gary Tsai
 **/
@Configuration
@ConfigurationProperties(prefix = "security.ldap")
@Data
public class LdapPropertyConfig {
    /**
     * LDAP 伺服器的 IP 位址或主機名稱。
     * 對應設定鍵：{@code security.ldap.ip}
     */
    private String ip;
    /**
     * LDAP 網域名稱，例如 {@code zipe.local}。
     * 對應設定鍵：{@code security.ldap.domain}
     */
    private String domain;
    /**
     * LDAP 伺服器連接埠，預設通常為 {@code 389}（LDAP）或 {@code 636}（LDAPS）。
     * 對應設定鍵：{@code security.ldap.port}
     */
    private String port;
    /**
     * LDAP 識別名稱（Distinguished Name）根節點。
     * 格式範例：{@code DC=zipe,DC=local}
     * 對應設定鍵：{@code security.ldap.dn}
     * sample:DC=zipe,DC=local
     */
    private String dn = "DC=zipe,DC=local";
}
