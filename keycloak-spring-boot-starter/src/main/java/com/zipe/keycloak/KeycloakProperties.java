package com.zipe.keycloak;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * Keycloak 設定屬性載體。
 *
 * <p>透過 {@code @ConfigurationProperties(prefix = "keycloak")} 將 {@code application.yml}
 * 中所有 {@code keycloak.*} 的鍵值對自動繫結至此 {@link HashMap}，
 * 供嵌入式 Keycloak 伺服器啟動時讀取動態屬性。
 *
 * <p>繼承 {@link HashMap} 而非宣告具體欄位，是為了支援 Keycloak 大量且結構多變的設定鍵，
 * 避免每次新增屬性都需異動此類別。Lombok 的 {@code @Getter}/{@code @Setter}
 * 提供 Map 本身以外的標準存取方法。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties extends HashMap<String, Object> {
}
