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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import javax.sql.DataSource;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/14 下午 05:52
 **/
@AutoConfiguration
@PropertySource({"classpath:data-source.properties"})
@EnableJpaRepositories(
        basePackages = "${spring.jpa.base.packages}",
        entityManagerFactoryRef = "multiEntityManager",
        transactionManagerRef = "multiTransactionManager"
)
@ConditionalOnClass(DataSourcePropertyConfig.class)
@EnableConfigurationProperties(DataSourcePropertyConfig.class)
public class DataSourceConfigAutoConfiguration extends BaseDataSourceConfig {

    private final HibernateProperties hibernateProperties;

    @Autowired
    DataSourceConfigAutoConfiguration(Environment env, DataSourcePropertyConfig dynamicDataSource,
        HibernateProperties hibernateProperties) {
        super(env, dynamicDataSource);
        this.hibernateProperties = hibernateProperties;
    }

    private DataSource createDataSource(DynamicDataSourceConfig dataSource) {
        HikariConfig config = this.baseHikariConfig();
        return new HikariDataSource(setHikariConnection(config, dataSource));
    }

    private DataSource createAs400DataSource(DynamicDataSourceConfig dataSource) {
        // 因 AS400 使用 Hikari 額外設定時會發生錯誤，所以只有基本設定
        HikariConfig config = setHikariConnection(new HikariConfig(), dataSource);
        // AS400 語法不能使用 SELECT 1
        config.setConnectionTestQuery("VALUES 1");
        return new HikariDataSource(config);
    }

    private HikariConfig setHikariConnection(HikariConfig config, DynamicDataSourceConfig dataSource) {
        //資料來源
        config.setJdbcUrl(dataSource.getUrl());
        //使用者名稱
        config.setUsername(dataSource.getUsername());
        String dbPassword = dataSource.getPa55word();
        if (Boolean.TRUE.equals(dynamicDataSource.getIsEncrypt())) {
            CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
            dbPassword = cryptoUtil.decode(dbPassword);
        }
        //密碼
        config.setPassword(dbPassword);
        return config;
    }

    @Bean
    public DataSource dataSource() {

        Map<Object, Object> dataSourceMap = new HashMap<>(16);
        if(Objects.isNull(dynamicDataSource.getDataSourceMap())){
            throw new DataSourceLookupFailureException("請設定 data-source.properties 內容");
        }

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

    @Bean(name = "multiEntityManager")
    public LocalContainerEntityManagerFactoryBean multiEntityManager(@Qualifier("dataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(dynamicDataSource.getEntityScan());
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaProperties(this.additionalProperties());
        factory.afterPropertiesSet();
        return factory;
    }

    @Primary
    @Bean(name = "multiTransactionManager")
    public PlatformTransactionManager multiTransactionManager(@Qualifier("multiEntityManager") LocalContainerEntityManagerFactoryBean multiEntityManager) {
//        LocalContainerEntityManagerFactoryBean multiEntityManager = multiEntityManager(dataSource);
        return new JpaTransactionManager(Objects.requireNonNull(multiEntityManager.getObject()));
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcDaoSupport namedParameterJdbcDaoSupport(DataSource dataSource) {
        NamedParameterJdbcDaoSupport dao = new NamedParameterJdbcDaoSupport();
        dao.setDataSource(dataSource);
        return dao;
    }

    Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, hibernateProperties.getDdlAuto());

        return properties;
    }
}
