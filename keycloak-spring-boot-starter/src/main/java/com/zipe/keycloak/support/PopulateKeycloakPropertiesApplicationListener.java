package com.zipe.keycloak.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.List;

/**
 * 在 Spring 應用程式環境準備完成之前，將 Keycloak 預設設定載入至 Environment。
 *
 * <p>此監聽器監聽 {@link ApplicationEnvironmentPreparedEvent} 事件，
 * 並嘗試從 classpath 讀取 {@code keycloak-defaults.yml}，
 * 將其中的屬性以最低優先權（addLast）加入 {@link ConfigurableEnvironment}，
 * 確保應用程式自訂設定可覆蓋這些預設值。
 * Loads the default keycloak configuration into the environment.
 */
@Slf4j
public class PopulateKeycloakPropertiesApplicationListener implements ApplicationListener<ApplicationEvent> {

    /**
     * 處理應用程式事件，僅在收到 {@link ApplicationEnvironmentPreparedEvent} 時才執行載入邏輯。
     *
     * <p>若 classpath 中不存在 {@code keycloak-defaults.yml}，則直接略過，不拋出例外。
     * 若讀取 YAML 檔案時發生 {@link IOException}，則包裝為 {@link RuntimeException} 重新拋出。
     *
     * @param event 應用程式事件；只有 {@link ApplicationEnvironmentPreparedEvent} 類型才會實際處理
     * @throws RuntimeException 當載入 YAML 屬性來源發生 I/O 錯誤時
     */
    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {

        // 非 ApplicationEnvironmentPreparedEvent 事件一律略過，避免干擾其他生命週期階段
        if (!(event instanceof ApplicationEnvironmentPreparedEvent)) {
            return;
        }

        ApplicationEnvironmentPreparedEvent envEvent = (ApplicationEnvironmentPreparedEvent) event;
        ConfigurableEnvironment env = envEvent.getEnvironment();

        try {
            Resource resource = new ClassPathResource("keycloak-defaults.yml");

            // 若預設設定檔不存在（例如非嵌入式 Keycloak 情境），直接結束不做任何處理
            if (!resource.exists()) {
                return;
            }

            log.info("Loading default keycloak properties configuration from: {}", resource.getURI());

            YamlPropertySourceLoader sourceLoader = new YamlPropertySourceLoader();
            List<PropertySource<?>> yamlTestProperties = sourceLoader.load("keycloak-defaults.yml", resource);
            if (!yamlTestProperties.isEmpty()) {
                // 使用 addLast 將預設值置於最低優先權，確保應用程式自訂設定可覆蓋
                env.getPropertySources().addLast(yamlTestProperties.get(0));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
