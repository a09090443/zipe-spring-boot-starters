package com.zipe.keycloak.support;

import lombok.extern.slf4j.Slf4j;
import org.infinispan.manager.DefaultCacheManager;
import org.springframework.beans.BeanUtils;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;

/**
 * 動態 JNDI Context 工廠建構器，用於在嵌入式 Keycloak 環境中提供 JNDI 查找支援。
 * <p>
 * 實作 {@link InitialContextFactoryBuilder}，將 Spring 管理的 {@link DataSource}、
 * Infinispan {@link DefaultCacheManager} 與 {@link ExecutorService} 綁定至預先定義的
 * JNDI 名稱，使 Keycloak 內部得以透過 JNDI 查找這些資源。
 * <p>
 * 此類別在 {@code @PostConstruct} 階段呼叫 {@link NamingManager#setInitialContextFactoryBuilder}
 * 完成全域 JNDI 工廠的替換，確保後續所有 JNDI 查找均由本實作負責路由。
 */
@Slf4j
public class DynamicJndiContextFactoryBuilder implements InitialContextFactoryBuilder {

    /** Keycloak 查找 Spring DataSource 時所用的 JNDI 名稱。 */
    public static final String JNDI_SPRING_DATASOURCE = "spring/datasource";

    /** Keycloak 查找 Infinispan CacheManager 時所用的 JNDI 名稱（注意：常數名拼字為歷史遺留）。 */
    public static final String JNDI_CACHE_MANAGAER = "spring/infinispan/cacheManager";

    /** Keycloak 查找執行緒池（ExecutorService）時所用的 JNDI 名稱，對應 JBoss EE 並行執行器路徑。 */
    public static final String JNDI_EXECUTOR_SERVICE = "java:jboss/ee/concurrency/executor/storage-provider-threads";

    /** 預先建立並固定繫結 Spring 資源的 {@link InitialContext}，供 JNDI 查找使用。 */
    private final InitialContext fixedInitialContext;

    /**
     * 建構 {@code DynamicJndiContextFactoryBuilder}，並立即建立固定的 {@link InitialContext}。
     *
     * @param dataSource      Spring 管理的資料來源，綁定至 {@link #JNDI_SPRING_DATASOURCE}
     * @param cacheManager    Infinispan 快取管理器，綁定至 {@link #JNDI_CACHE_MANAGAER}
     * @param executorService 執行緒池服務，綁定至 {@link #JNDI_EXECUTOR_SERVICE}
     */
    public DynamicJndiContextFactoryBuilder(DataSource dataSource, DefaultCacheManager cacheManager, ExecutorService executorService) {
        fixedInitialContext = createFixedInitialContext(dataSource, cacheManager, executorService);
    }

    /**
     * 建立並回傳一個預先綁定 Spring 資源的 {@link InitialContext}。
     * <p>
     * 將傳入的三個資源以 {@link Hashtable} 封裝為 JNDI 環境，
     * 再交由 {@link KeycloakInitialContext} 建立可供查找的 Context 實例。
     *
     * @param dataSource      要綁定的資料來源
     * @param cacheManager    要綁定的 Infinispan 快取管理器
     * @param executorService 要綁定的執行緒池服務
     * @return 包含上述資源綁定的 {@link InitialContext}
     * @throws RuntimeException 若 {@link KeycloakInitialContext} 初始化失敗
     */
    protected InitialContext createFixedInitialContext(DataSource dataSource, DefaultCacheManager cacheManager, ExecutorService executorService) {

        Hashtable<Object, Object> jndiEnv = new Hashtable<>();
        jndiEnv.put(JNDI_SPRING_DATASOURCE, dataSource);
        jndiEnv.put(JNDI_CACHE_MANAGAER, cacheManager);
        jndiEnv.put(JNDI_EXECUTOR_SERVICE, executorService);

        try {
            return new KeycloakInitialContext(jndiEnv);
        } catch (NamingException ne) {
            throw new RuntimeException("Could not create KeycloakInitialContext", ne);
        }
    }

    /**
     * 在 Bean 初始化完成後，將本實例設定為全域的 {@link InitialContextFactoryBuilder}。
     * <p>
     * 呼叫 {@link NamingManager#setInitialContextFactoryBuilder} 會取代 JVM 預設的 JNDI 工廠機制，
     * 使後續所有 JNDI 查找均先通過本類別的 {@link #createInitialContextFactory} 進行路由。
     * 若設定失敗，僅記錄錯誤日誌而不中斷應用程式啟動，以免影響整體服務可用性。
     */
    @PostConstruct
    public void init() {
        try {
            NamingManager.setInitialContextFactoryBuilder(this);
        } catch (NamingException e) {
            log.error("Could not configure InitialContextFactoryBuilder", e);
        }
    }


    /**
     * Create a new {@link InitialContextFactory} based on the given {@code environment}.
     * <p>
     * If the lookup environment is empty, we return a JndiContextFactory that returns a InitialContext which supports a lookups against a fixed set of Spring beans.
     * If the environment is not empty, we try to use the provided java.naming.factory.initial classname to create a JndiContextFactory and
     * delegate further lookups to this instance. Otherwise we simply return {@literal null}.
     *
     * @param environment JNDI 查找時傳入的環境參數；若為 {@code null} 或空值，
     *                    則回傳直接指向 {@link #fixedInitialContext} 的工廠
     * @return 對應的 {@link InitialContextFactory}；若無法解析則回傳 {@code null}
     */
    @Override
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) {

        if (environment == null || environment.isEmpty()) {
            return env -> fixedInitialContext;
        }

        String factoryClassName = (String) environment.get("java.naming.factory.initial");
        if (factoryClassName != null) {
            try {
                // factoryClassName -> com.sun.jndi.ldap.LdapCtxFactory
                Class<?> factoryClass = Thread.currentThread().getContextClassLoader().loadClass(factoryClassName);
                return BeanUtils.instantiateClass(factoryClass, InitialContextFactory.class);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
