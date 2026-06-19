package com.zipe.keycloak.support;

import com.google.auto.service.AutoService;
import org.infinispan.manager.DefaultCacheManager;
import org.keycloak.Config;
import org.keycloak.cluster.ManagedCacheManagerProvider;
import org.springframework.jndi.JndiTemplate;

import javax.naming.NamingException;

/**
 * Infinispan 快取管理器提供者。
 *
 * <p>實作 Keycloak 的 {@link ManagedCacheManagerProvider} 介面，
 * 透過 JNDI 查找方式取得由 {@link DynamicJndiContextFactoryBuilder}
 * 預先註冊的 {@link DefaultCacheManager} 實例，
 * 讓嵌入式 Keycloak 可正確使用 Infinispan 進行叢集快取。</p>
 *
 * <p>使用 {@code @AutoService} 自動將此類別註冊為 SPI 實作，
 * 無需手動在 META-INF/services 中宣告。</p>
 */
@AutoService(ManagedCacheManagerProvider.class)
public class InfinispanCacheManagerProvider implements ManagedCacheManagerProvider {

    /**
     * 透過 JNDI 取得 Infinispan {@link DefaultCacheManager} 實例。
     *
     * <p>查找鍵名由 {@link DynamicJndiContextFactoryBuilder#JNDI_CACHE_MANAGAER} 提供，
     * 對應在 Spring Boot 啟動階段預先綁定至 JNDI Context 的快取管理器。</p>
     *
     * @param <C>    快取管理器型別，由呼叫端決定
     * @param config Keycloak 設定範圍（本實作未使用，由 JNDI 直接提供實例）
     * @return 已初始化的 {@link DefaultCacheManager} 實例
     * @throws RuntimeException 當 JNDI 查找失敗（{@link NamingException}）時包裝拋出
     */
    @Override
    @SuppressWarnings("unchecked")
    public <C> C getCacheManager(Config.Scope config) {
        try {
            // 透過 JNDI 查找預先註冊的 Infinispan DefaultCacheManager，
            // 確保嵌入式 Keycloak 與 Spring Boot 共用同一個快取管理器實例
            return (C) new JndiTemplate().lookup(DynamicJndiContextFactoryBuilder.JNDI_CACHE_MANAGAER, DefaultCacheManager.class);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
