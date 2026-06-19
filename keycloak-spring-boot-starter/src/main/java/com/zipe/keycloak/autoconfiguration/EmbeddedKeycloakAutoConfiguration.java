package com.zipe.keycloak.autoconfiguration;

import com.zipe.keycloak.EmbeddedKeycloakApplication;
import com.zipe.keycloak.EmbeddedKeycloakServer;
import com.zipe.keycloak.KeycloakCustomProperties;
import com.zipe.keycloak.KeycloakProperties;
import com.zipe.keycloak.support.DynamicJndiContextFactoryBuilder;
import com.zipe.keycloak.support.KeycloakUndertowRequestFilter;
import com.zipe.keycloak.support.SpringBootConfigProvider;
import com.zipe.keycloak.support.SpringBootPlatformProvider;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.keycloak.platform.Platform;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.servlet.Filter;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 嵌入式 Keycloak 的 Spring Boot 自動配置類別。
 *
 * <p>負責在 Spring Boot 應用啟動時，自動初始化並註冊嵌入式 Keycloak 所需的各項 Bean，
 * 包含伺服器實例、平台提供者、設定提供者、JNDI 工廠、Infinispan 快取管理員、
 * JAX-RS Servlet 以及 Session 管理過濾器。</p>
 *
 * <p>所有 Bean 均使用 {@link ConditionalOnMissingBean} 保護，允許使用者在業務系統中
 * 自訂同名 Bean 以覆寫預設行為。</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({KeycloakProperties.class, KeycloakCustomProperties.class})
public class EmbeddedKeycloakAutoConfiguration {

    /**
     * 建立並註冊嵌入式 Keycloak 伺服器實例。
     *
     * @param serverProperties Spring Boot 伺服器屬性（用於取得 port 等設定）
     * @return {@link EmbeddedKeycloakServer} 實例
     */
    @Bean
    @ConditionalOnMissingBean(name = "embeddedKeycloakServer")
    protected EmbeddedKeycloakServer embeddedKeycloakServer(ServerProperties serverProperties) {
        return new EmbeddedKeycloakServer(serverProperties);
    }

    /**
     * 取得並註冊 Spring Boot 平台提供者。
     *
     * <p>Keycloak 在啟動時會透過 {@link Platform#getPlatform()} 取得平台抽象，
     * 此 Bean 確保回傳的是針對 Spring Boot 環境客製化的 {@link SpringBootPlatformProvider}。</p>
     *
     * @return {@link SpringBootPlatformProvider} 實例
     */
    @Bean
    @ConditionalOnMissingBean(name = "springBootPlatform")
    protected SpringBootPlatformProvider springBootPlatform() {
        return (SpringBootPlatformProvider) Platform.getPlatform();
    }

    /**
     * 建立並註冊 Spring Boot 設定提供者。
     *
     * <p>將 {@link KeycloakProperties} 中的設定橋接給嵌入式 Keycloak，
     * 使其能從 Spring Boot 的 application.yml / application.properties 讀取 Keycloak 相關屬性。</p>
     *
     * @param keycloakProperties Keycloak 屬性設定
     * @return {@link SpringBootConfigProvider} 實例
     */
    @Bean
    @ConditionalOnMissingBean(name = "springBootConfigProvider")
    protected SpringBootConfigProvider springBootConfigProvider(KeycloakProperties keycloakProperties) {
        return new SpringBootConfigProvider(keycloakProperties);
    }

    /**
     * 建立並註冊動態 JNDI 工廠建構器。
     *
     * <p>Keycloak 內部透過 JNDI 查找資料來源與快取管理員，
     * 此 Bean 將 Spring 管理的 {@link DataSource}、{@link DefaultCacheManager}
     * 以及執行緒池綁定至 JNDI 環境，使 Keycloak 能正確取得這些資源。</p>
     *
     * @param dataSource      應用程式資料來源
     * @param cacheManager    Infinispan 快取管理員
     * @param executorService 固定大小執行緒池（由 {@code fixedThreadPool} Bean 提供）
     * @return {@link DynamicJndiContextFactoryBuilder} 實例
     */
    @Bean
    @ConditionalOnMissingBean(name = "springBeansJndiContextFactory")
    protected DynamicJndiContextFactoryBuilder springBeansJndiContextFactory(DataSource dataSource, DefaultCacheManager cacheManager, @Qualifier("fixedThreadPool") ExecutorService executorService) {
        return new DynamicJndiContextFactoryBuilder(dataSource, cacheManager, executorService);
    }

    /**
     * 建立供 Keycloak 內部使用的固定大小執行緒池。
     *
     * <p>執行緒數量固定為 5，供 JNDI 工廠及 Keycloak 非同步任務使用。
     * 若預設容量不符合需求，可自行定義同名 Bean 覆寫。</p>
     *
     * @return 固定大小為 5 的 {@link ExecutorService}
     */
    @Bean("fixedThreadPool")
    public ExecutorService fixedThreadPool() {
        return Executors.newFixedThreadPool(5);
    }

    /**
     * 建立並啟動 Infinispan 分散式快取管理員。
     *
     * <p>從 {@link KeycloakCustomProperties.Infinispan#getConfigLocation()} 指定的資源路徑
     * 解析 Infinispan XML 設定，並以非自動啟動模式建立 {@link DefaultCacheManager} 後手動呼叫 start()。
     * Keycloak 使用 Infinispan 快取儲存 Session 及令牌等狀態資訊。</p>
     *
     * @param customProperties 自訂 Keycloak 屬性（含 Infinispan 設定位置）
     * @return 已啟動的 {@link DefaultCacheManager} 實例
     * @throws Exception 當 Infinispan 設定檔無法讀取或解析失敗時拋出
     */
    @Bean
    @ConditionalOnMissingBean(name = "keycloakInfinispanCacheManager")
    protected DefaultCacheManager keycloakInfinispanCacheManager(KeycloakCustomProperties customProperties) throws Exception {

        KeycloakCustomProperties.Infinispan infinispan = customProperties.getInfinispan();
        Resource configLocation = infinispan.getConfigLocation();
        log.info("Using infinispan configuration from {}", configLocation.getURI());

        // 解析 Infinispan XML 設定，以 false 表示不自動啟動，確保完成設定後再手動 start()
        ConfigurationBuilderHolder configBuilder = new ParserRegistry().parse(configLocation.getURL());
        DefaultCacheManager defaultCacheManager = new DefaultCacheManager(configBuilder, false);
        defaultCacheManager.start();
        return defaultCacheManager;
    }

    /**
     * 建立並註冊 Keycloak JAX-RS（RESTEasy）Servlet。
     *
     * <p>將 {@link HttpServlet30Dispatcher} 作為 Keycloak 的 REST API 入口，
     * 並依照 {@link KeycloakCustomProperties} 中設定的路徑進行 URL 映射。
     * 啟動前會先呼叫 {@link #initKeycloakEnvironmentFromProfiles()} 載入功能旗標設定。</p>
     *
     * <p>安全相關的 RESTEasy 參數（停用 DTD、停用外部實體展開）在此一併設定，
     * 以防範 XXE 攻擊。</p>
     *
     * @param customProperties 自訂 Keycloak 屬性（含 Keycloak 路徑設定）
     * @return 已設定完成的 {@link ServletRegistrationBean}
     */
    @Bean
    @ConditionalOnMissingBean(name = "keycloakJaxRsApplication")
    protected ServletRegistrationBean<HttpServlet30Dispatcher> keycloakJaxRsApplication(KeycloakCustomProperties customProperties) {

        // 在註冊 Servlet 前先從 profile.properties 載入 Keycloak 功能旗標，避免以預設值覆蓋系統屬性
        initKeycloakEnvironmentFromProfiles();

        ServletRegistrationBean<HttpServlet30Dispatcher> servlet = new ServletRegistrationBean<>(new HttpServlet30Dispatcher());
        servlet.addInitParameter("javax.ws.rs.Application", EmbeddedKeycloakApplication.class.getName());

        // 啟用 Gzip 壓縮以減少回應資料量
        servlet.addInitParameter("resteasy.allowGzip", "true");
        servlet.addInitParameter("keycloak.embedded", "true");
        // 停用外部實體展開與 DTD，防範 XXE（XML External Entity）攻擊
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_EXPAND_ENTITY_REFERENCES, "false");
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_SECURE_PROCESSING_FEATURE, "true");
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_DISABLE_DTDS, "true");
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, customProperties.getServer().getKeycloakPath());
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS, "false");
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_DISABLE_HTML_SANITIZER, "true");
        servlet.addUrlMappings(customProperties.getServer().getKeycloakPath() + "/*");

        // loadOnStartup=2 確保在應用啟動初期即初始化 Keycloak，不等待第一次請求
        servlet.setLoadOnStartup(2);
        servlet.setAsyncSupported(true);

        return servlet;
    }

    /**
     * 從 classpath 根目錄的 {@code profile.properties} 載入 Keycloak 功能旗標。
     *
     * <p>將各屬性以 {@code keycloak.profile.<featureName>} 格式設定為 JVM 系統屬性，
     * 用於控制 Keycloak 的預覽功能（Preview Feature）開關。
     * 若系統屬性已存在則不覆寫，優先採用外部傳入的設定。
     * 若找不到 {@code profile.properties} 檔案，則靜默略過。</p>
     */
    private void initKeycloakEnvironmentFromProfiles() {

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("profile.properties")) {

            if (in == null) {
                log.info("Could not find profile.properties on classpath.");
                return;
            }

            Properties profile = new Properties();
            profile.load(in);

            log.info("Found profile.properties on classpath.");
            String profilePrefix = "keycloak.profile.";
            for (Object key : profile.keySet()) {
                String value = (String) profile.get(key);
                // 功能名稱統一轉小寫，確保大小寫不敏感
                String featureName = key.toString().toLowerCase();
                String currentValue = System.getProperty(profilePrefix + featureName);
                // 僅在尚未設定時才套用，避免覆蓋啟動參數或外部環境傳入的設定
                if (currentValue == null) {
                    System.setProperty(profilePrefix + featureName, value);
                }
            }
        } catch (IOException ioe) {
            log.warn("Could not read profile.properties.", ioe);
        }
    }

    /**
     * 建立並註冊 Keycloak Session 管理過濾器。
     *
     * <p>將 {@link KeycloakUndertowRequestFilter} 套用至 Keycloak 路徑下的所有請求，
     * 確保每個請求在進入 Keycloak 前完成必要的 Undertow 請求上下文初始化，
     * 以支援 Session 的正確管理。</p>
     *
     * @param customProperties 自訂 Keycloak 屬性（含 Keycloak 路徑設定）
     * @return 已設定完成的 {@link FilterRegistrationBean}
     */
    @Bean
    @ConditionalOnMissingBean(name = "keycloakSessionManagement")
    protected FilterRegistrationBean<Filter> keycloakSessionManagement(KeycloakCustomProperties customProperties) {

        FilterRegistrationBean<Filter> filter = new FilterRegistrationBean<>();
        filter.setName("Keycloak Session Management");
        filter.setFilter(new KeycloakUndertowRequestFilter());
        filter.addUrlPatterns(customProperties.getServer().getKeycloakPath() + "/*");

        return filter;
    }

}
