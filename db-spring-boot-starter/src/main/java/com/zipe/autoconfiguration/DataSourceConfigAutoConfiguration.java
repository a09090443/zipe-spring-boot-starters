package com.zipe.autoconfiguration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zipe.base.config.DataSourcePropertyConfig;
import com.zipe.base.database.BaseDataSourceConfig;
import com.zipe.base.database.DataSourceHolder;
import com.zipe.base.database.DynamicDataSource;
import com.zipe.base.model.DynamicDataSourceConfig;
import com.zipe.util.crypto.Base64Util;
import com.zipe.util.crypto.CryptoUtil;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * 資料來源自動配置類別。
 *
 * <p>在 Spring Boot 啟動時自動讀取 {@code data-source.properties}，
 * 依配置建立一個或多個 HikariCP 連線池，並組裝成 {@link DynamicDataSource}
 * 以支援執行期間的多資料來源動態切換。同時負責建立 JPA {@code EntityManagerFactory}、
 * 交易管理器（{@code PlatformTransactionManager}）、
 * {@link org.springframework.jdbc.core.JdbcTemplate} 及
 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport} 等 Bean。</p>
 *
 * <p>另透過 {@link DynamicJpaRepositoriesRegistrar} 支援以 {@code dynamic.base-packages}
 * 設定檔屬性驅動 JPA Repository 掃描，免去引入自帶 {@code @EnableJpaRepositories} 的模組
 * （如 iam-starter）後，須在啟動類別手動補宣告 {@code @EnableJpaRepositories} 的負擔。</p>
 *
 * <p>僅在 {@link DataSourcePropertyConfig} 類別存在於 classpath 時才會生效。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/14 下午 05:52
 **/
@AutoConfiguration
@PropertySource({"classpath:data-source.properties"})
@ConditionalOnClass(DataSourcePropertyConfig.class)
@EnableConfigurationProperties(DataSourcePropertyConfig.class)
@Import(DynamicJpaRepositoriesRegistrar.class)
public class DataSourceConfigAutoConfiguration extends BaseDataSourceConfig {

    /** Hibernate DDL 與方言等附加屬性，由 Spring Boot 自動注入。 */
    private final HibernateProperties hibernateProperties;

    /**
     * 建構子，由 Spring 容器自動注入所需依賴。
     *
     * @param env               Spring 環境物件，用於讀取 properties 設定值
     * @param dynamicDataSource 多資料來源屬性設定，來自 {@code data-source.properties}
     * @param hibernateProperties Hibernate 相關屬性（如 DDL 策略）
     */
    DataSourceConfigAutoConfiguration(Environment env, DataSourcePropertyConfig dynamicDataSource,
                                      HibernateProperties hibernateProperties) {
        super(env, dynamicDataSource);
        this.hibernateProperties = hibernateProperties;
    }

    /**
     * 建立一般資料庫的 HikariCP 資料來源。
     *
     * <p>使用 {@link BaseDataSourceConfig#baseHikariConfig()} 取得含 pool size、
     * timeout 等預設調校參數的基底配置，再套用連線資訊後建立連線池。</p>
     *
     * @param dataSource 資料來源設定模型（含 URL、帳號、密碼）
     * @return 已初始化的 {@link HikariDataSource}
     */
    private DataSource createDataSource(DynamicDataSourceConfig dataSource) {
        HikariConfig config = this.baseHikariConfig();
        return new HikariDataSource(setHikariConnection(config, dataSource));
    }

    /**
     * 建立 IBM AS/400（DB2 for i）專用的 HikariCP 資料來源。
     *
     * <p>AS/400 驅動程式與部分 Hikari 進階選項不相容，因此刻意略過
     * {@link BaseDataSourceConfig#baseHikariConfig()} 的預設調校，
     * 以空的 {@link HikariConfig} 作為起點，僅套用基本連線參數。</p>
     *
     * @param dataSource 資料來源設定模型（含 URL、帳號、密碼）
     * @return 已初始化的 {@link HikariDataSource}，連線測試語法為 {@code VALUES 1}
     */
    private DataSource createAs400DataSource(DynamicDataSourceConfig dataSource) {
        // 因 AS400 使用 Hikari 額外設定時會發生錯誤，所以只有基本設定
        HikariConfig config = setHikariConnection(new HikariConfig(), dataSource);
        // AS400 語法不能使用 SELECT 1
        config.setConnectionTestQuery("VALUES 1");
        return new HikariDataSource(config);
    }

    /**
     * 將 {@link DynamicDataSourceConfig} 中的連線資訊填入 {@link HikariConfig}。
     *
     * <p>若全域設定 {@code isEncrypt=true}，會先以 Base64 解碼密碼再套用，
     * 避免明文密碼直接寫入設定檔。</p>
     *
     * @param config     待填入連線資訊的 HikariCP 設定物件（可已含其他調校參數）
     * @param dataSource 來源設定模型，提供 JDBC URL、帳號與密碼
     * @return 填入連線資訊後的 {@code config}，供後續建立 {@link HikariDataSource} 使用
     */
    private HikariConfig setHikariConnection(HikariConfig config, DynamicDataSourceConfig dataSource) {
        //資料來源
        config.setJdbcUrl(dataSource.getUrl());
        //使用者名稱
        config.setUsername(dataSource.getUsername());
        String dbPassword = dataSource.getPa55word();
        // 若啟用加密模式，需先解碼後才能用於連線；避免明文密碼存於設定檔
        if (Boolean.TRUE.equals(dynamicDataSource.getIsEncrypt())) {
            CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
            dbPassword = cryptoUtil.decode(dbPassword);
        }
        //密碼
        config.setPassword(dbPassword);
        return config;
    }

    /**
     * 建立並註冊 {@link DynamicDataSource} Bean。
     *
     * <p>遍歷 {@code data-source.properties} 中設定的所有資料來源：
     * <ul>
     *   <li>URL 含有 {@code as400}（不分大小寫）的資料來源使用 AS/400 專屬建立方式。</li>
     *   <li>其餘資料來源使用含完整 Hikari 調校參數的標準建立方式。</li>
     * </ul>
     * 每個資料來源的鍵值也會同步加入 {@link DataSourceHolder#dataSourceNames}，
     * 供 AOP 切面動態切換時查詢。</p>
     *
     * @return 已配置好所有子資料來源與預設資料來源的 {@link DynamicDataSource}
     * @throws DataSourceLookupFailureException 若 {@code data-source.properties} 未設定任何資料來源
     */
    @Bean
    public DataSource dataSource() {

        Map<Object, Object> dataSourceMap = new HashMap<>(16);
        if (Objects.isNull(dynamicDataSource.getDataSourceMap())) {
            throw new DataSourceLookupFailureException("請設定 data-source.properties 內容");
        }

        // 依 URL 判斷資料庫類型，分別建立對應的 HikariCP 連線池，並記錄資料來源名稱供切換使用
        Optional.of(dynamicDataSource.getDataSourceMap()).ifPresent(dataSource -> dataSource.forEach((k, v) -> {
            if (!v.getUrl().toLowerCase().contains("as400")) {
                dataSourceMap.put(k, createDataSource(v));
            } else {
                dataSourceMap.put(k, createAs400DataSource(v));
            }

            DataSourceHolder.dataSourceNames.add(k);
        }));
        DynamicDataSource dataSource = new DynamicDataSource();
        //設定資料來源對映
        dataSource.setTargetDataSources(dataSourceMap);
        //設定預設資料來源，當無法對映到資料來源時會使用預設資料來源
        dataSource.setDefaultTargetDataSource(dataSourceMap.get(dynamicDataSource.getPrimary()));
        dataSource.afterPropertiesSet();
        return dataSource;
    }

    /**
     * 建立 JPA {@code EntityManagerFactory} Bean。
     *
     * <p>以 {@link DynamicDataSource} 作為資料來源，掃描 Entity 套件，並套用
     * {@link #additionalProperties()} 中的 Hibernate 附加屬性。</p>
     *
     * <p>掃描的 Entity 套件由兩個來源合併而成：</p>
     * <ol>
     *   <li>{@code data-source.properties} 的 {@code dynamic.entity-scan}（支援逗號分隔多套件）。</li>
     *   <li>容器中所有以 {@code @EntityScan} 註冊的套件（{@link EntityScanPackages}）。因 db-starter
     *       自建此 {@code EntityManagerFactory} 會使 Spring Boot 預設 JPA 自動配置退讓，連帶讓
     *       {@code @EntityScan} 失效；此處主動併入，讓引入如 iam-starter 等以 {@code @EntityScan}
     *       宣告 Entity 套件的 starter 時，<b>無須在 {@code dynamic.entity-scan} 重複設定</b>其套件。</li>
     * </ol>
     *
     * @param dataSource  已配置好的動態資料來源（由 {@link #dataSource()} 提供）
     * @param beanFactory 容器，用於讀取 {@link EntityScanPackages} 的 {@code @EntityScan} 套件
     * @return 初始化完成的 {@link LocalContainerEntityManagerFactoryBean}
     */
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean multiEntityManager(@Qualifier("dataSource") DataSource dataSource,
                                                                     BeanFactory beanFactory) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(resolveEntityScanPackages(beanFactory));
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaProperties(this.additionalProperties());
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * 解析 EntityManagerFactory 要掃描的 Entity 套件清單。
     *
     * <p>合併 {@code dynamic.entity-scan}（逗號分隔，去除空白）與容器中所有
     * {@code @EntityScan} 註冊的套件，並去除重複、保留加入順序。</p>
     *
     * @param beanFactory 容器，用於取得 {@link EntityScanPackages}
     * @return 去重後的 Entity 掃描套件陣列
     */
    private String[] resolveEntityScanPackages(BeanFactory beanFactory) {
        Set<String> packages = new LinkedHashSet<>();
        // 1) 明確設定的 dynamic.entity-scan（逗號分隔可指定多個套件，單一套件亦相容）
        if (StringUtils.hasText(dynamicDataSource.getEntityScan())) {
            Collections.addAll(packages,
                    StringUtils.trimArrayElements(
                            StringUtils.commaDelimitedListToStringArray(dynamicDataSource.getEntityScan())));
        }
        // 2) 併入所有以 @EntityScan 註冊的套件（如 iam-starter 的 com.zipe.entity）
        packages.addAll(EntityScanPackages.get(beanFactory).getPackageNames());
        return packages.toArray(new String[0]);
    }

    /**
     * 建立主要的 JPA 交易管理器 Bean，並標記為 {@code @Primary}。
     *
     * <p>以 {@link LocalContainerEntityManagerFactoryBean#getObject()} 取得
     * {@code EntityManagerFactory} 後建立 {@link JpaTransactionManager}，
     * 確保多資料來源場景下預設使用此交易管理器。</p>
     *
     * @param multiEntityManager 已初始化的 EntityManagerFactory 容器 Bean
     * @return 配置完成的 {@link PlatformTransactionManager}
     */
    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean multiEntityManager) {
        return new JpaTransactionManager(Objects.requireNonNull(multiEntityManager.getObject()));
    }

    /**
     * 建立 {@link JdbcTemplate} Bean，供需要直接執行 SQL 的元件注入使用。
     *
     * @param dataSource 動態資料來源
     * @return 以動態資料來源初始化的 {@link JdbcTemplate}
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 建立 {@link NamedParameterJdbcDaoSupport} Bean，
     * 提供具名參數（{@code :paramName}）的 JDBC 操作支援。
     *
     * @param dataSource 動態資料來源
     * @return 已設定資料來源的 {@link NamedParameterJdbcDaoSupport}
     */
    @Bean
    public NamedParameterJdbcDaoSupport namedParameterJdbcDaoSupport(DataSource dataSource) {
        NamedParameterJdbcDaoSupport dao = new NamedParameterJdbcDaoSupport();
        dao.setDataSource(dataSource);
        return dao;
    }

    /**
     * 組裝並回傳傳遞給 Hibernate 的附加屬性。
     *
     * <p>目前僅設定 {@code hibernate.hbm2ddl.auto}，
     * 若 {@link HibernateProperties#getDdlAuto()} 未設定則預設為 {@code none}，
     * 以避免自動 DDL 操作意外修改生產資料庫結構。</p>
     *
     * @return 含 Hibernate 附加設定的 {@link Properties} 物件
     */
    Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, hibernateProperties.getDdlAuto() == null ? "none" : hibernateProperties.getDdlAuto());
        return properties;
    }
}
