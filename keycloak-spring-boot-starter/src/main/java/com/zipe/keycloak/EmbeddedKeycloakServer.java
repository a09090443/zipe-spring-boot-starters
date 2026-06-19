package com.zipe.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

/**
 * 嵌入式 Keycloak 伺服器的 Spring Boot 整合入口類別。
 * <p>
 * 負責在 Spring Boot 應用程式完全就緒後，印出目前 Keycloak 的版本資訊、
 * 各類功能啟用狀態（已棄用、預覽、實驗性、停用），以及可供存取的管理介面 URL。
 * </p>
 */
@JBossLog
@RequiredArgsConstructor
public class EmbeddedKeycloakServer {

    /** 注入 Spring Boot 伺服器設定，用於取得埠號與 Context Path。 */
    private final ServerProperties serverProperties;

    /**
     * 建立並回傳一個 {@link ApplicationListener}，
     * 在 {@link ApplicationReadyEvent} 觸發時（即應用程式完全啟動後）
     * 印出 Keycloak 版本與功能啟用資訊，以及嵌入式 Keycloak 管理介面的存取網址。
     *
     * @return 監聽 {@link ApplicationReadyEvent} 的 Spring Bean 監聽器
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> onApplicationReadyEventListener() {

        return (evt) -> {

            log.infof("Using Keycloak Version: %s", Version.VERSION_KEYCLOAK);
            log.infof("Enabled Keycloak Features (Deprecated): %s", Profile.getDeprecatedFeatures());
            log.infof("Enabled Keycloak Features (Preview): %s", Profile.getPreviewFeatures());
            log.infof("Enabled Keycloak Features (Experimental): %s", Profile.getExperimentalFeatures());
            log.infof("Enabled Keycloak Features (Disabled): %s", Profile.getDisabledFeatures());

            // 取得目前伺服器監聽的埠號，組合成可直接點擊的管理介面 URL
            Integer port = serverProperties.getPort();

            log.infof("Embedded Keycloak started: Browse to <http://localhost:%d%s> to use keycloak%n", port, serverProperties.getServlet().getContextPath());
        };
    }
}
