package com.zipe.keycloak;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Keycloak 自訂屬性設定類別，對應 {@code application.yml} 中 {@code keycloak.custom} 前綴的設定。
 * <p>
 * 包含伺服器路徑、管理員帳號、資料匯入（Migration）以及 Infinispan 快取等子群組設定，
 * 供嵌入式 Keycloak 服務在啟動時讀取並套用。
 * </p>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "keycloak.custom")
public class KeycloakCustomProperties {

    /** 伺服器路徑相關設定 */
    Server server = new Server();

    /** 管理員使用者相關設定 */
    AdminUser adminUser = new AdminUser();

    /** Realm 匯入（Migration）相關設定 */
    Migration migration = new Migration();

    /** Infinispan 分散式快取相關設定 */
    Infinispan infinispan = new Infinispan();

    /**
     * Keycloak 伺服器路徑設定，控制 JAX-RS 應用程式的對外掛載路徑。
     */
    @Getter
    @Setter
    public static class Server {

        /**
         * Path relative to {@code server.servlet.context-path} for the Keycloak JAX-RS Application to listen to.
         */
        String keycloakPath = "/auth";
    }

    /**
     * Keycloak Realm 匯入設定，用於指定初始化時要匯入的 Realm 設定檔來源與匯入策略。
     */
    @Getter
    @Setter
    public static class Migration {

        /**
         * Realm 設定檔的來源位置，預設為檔案系統路徑 {@code keycloak-realm-config.json}。
         * 若需從 classpath 讀取，可替換為 {@link org.springframework.core.io.ClassPathResource}。
         */
        Resource importLocation = new FileSystemResource("keycloak-realm-config.json");

        /**
         * 匯入策略提供者名稱，預設為 {@code singleFile}（整份 JSON 檔案一次匯入）。
         */
        String importProvider = "singleFile";
    }

    /**
     * Infinispan 分散式快取設定，指定快取設定檔的載入位置。
     */
    @Getter
    @Setter
    public static class Infinispan {

        /**
         * Infinispan 設定檔的來源位置，預設從 classpath 載入 {@code infinispan.xml}。
         */
        Resource configLocation = new ClassPathResource("infinispan.xml");
    }

    /**
     * Keycloak 管理員使用者設定，控制是否在啟動時自動建立預設管理員帳號。
     */
    @Getter
    @Setter
    public static class AdminUser {

        /**
         * 是否在 Keycloak 啟動時自動建立管理員使用者，預設為 {@code true}。
         * 若環境中已存在管理員帳號，可設為 {@code false} 以避免重複建立。
         */
        boolean createAdminUserEnabled = true;

        /** 管理員使用者名稱，預設為 {@code admin}。 */
        String username = "admin";

        /** 管理員密碼，建議透過環境變數或外部設定注入，不應明文寫在設定檔中。 */
        String password;
    }
}
