package com.zipe.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zipe.base.config.DataSourcePropertyConfig;
import com.zipe.base.database.BaseDataSourceConfig;
import com.zipe.base.database.DataSourceHolder;
import com.zipe.base.database.DynamicDataSource;
import com.zipe.base.model.DynamicDataSourceConfig;
import com.zipe.util.StringConstant;
import com.zipe.util.crypto.Base64Util;
import com.zipe.util.crypto.CryptoUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Configuration
@EnableJpaRepositories(
        basePackages = "${dynamic.base-packages}",
        entityManagerFactoryRef = "multiEntityManager",
        transactionManagerRef = "multiTransactionManager"
)
@EnableConfigurationProperties(DataSourcePropertyConfig.class)
public class DataSourceConfig extends BaseDataSourceConfig {

    DataSourceConfig(Environment env, DataSourcePropertyConfig dynamicDataSource) {
        super(env, dynamicDataSource);
    }

    private DataSource createDataSource(DynamicDataSourceConfig dynamicDataSource) {
        //資料來源
        baseHikariConfig().setJdbcUrl(dynamicDataSource.getUrl());
        //使用者名稱
        baseHikariConfig().setUsername(dynamicDataSource.getUsername());
        String dbPassword = dynamicDataSource.getPa55word();

        if (Objects.requireNonNull(env.getProperty("encrypt.enabled")).equalsIgnoreCase(StringConstant.TRUE)) {
            CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
            dbPassword = cryptoUtil.decode(dbPassword);
        }
        //密碼
        baseHikariConfig().setPassword(dbPassword);

        baseHikariConfig().setDriverClassName(dynamicDataSource.getDriverClassName());

        return new HikariDataSource(baseHikariConfig());
    }

    private DataSource createAs400DataSource(DynamicDataSourceConfig dynamicDataSource) {
        // 因 AS400 使用 Hikari 額外設定時會發生錯誤，所以只有基本設定
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dynamicDataSource.getUrl());                                           //資料來源
        config.setUsername(dynamicDataSource.getUsername());                                     //使用者名稱
        String dbPassword = dynamicDataSource.getPa55word();
        if (Objects.requireNonNull(env.getProperty("encrypt.enabled")).equalsIgnoreCase(StringConstant.TRUE)) {
            CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
            dbPassword = cryptoUtil.decode(dbPassword);
        }
        config.setPassword(dbPassword);                                                          //密碼
        config.setDriverClassName(dynamicDataSource.getDriverClassName());
        config.setConnectionTestQuery("VALUES 1");                                               // AS400 語法不能使用 SELECT 1

        return new HikariDataSource(config);
    }

    @Bean
    public DataSource dataSource() {

        Map<Object, Object> dataSourceMap = new HashMap<>();
        Optional.ofNullable(dynamicDataSource.getDataSourceMap()).ifPresent(dataSource -> {
            dataSource.forEach((k, v) -> {
                if (!v.getUrl().toLowerCase().contains("as400")) {
                    dataSourceMap.put(k, createDataSource(v));
                } else {
                    dataSourceMap.put(k, createAs400DataSource(v));
                }

                DataSourceHolder.dataSourceNames.add(k);
            });
        });
        DynamicDataSource dataSource = new DynamicDataSource();
        //設定資料來源對映
        dataSource.setTargetDataSources(dataSourceMap);
        //設定預設資料來源，當無法對映到資料來源時會使用預設資料來源
        dataSource.setDefaultTargetDataSource(dataSourceMap.get(dynamicDataSource.getPrimary()));
        dataSource.afterPropertiesSet();
        return dataSource;
    }

    @Bean(name = "multiEntityManager")
    public LocalContainerEntityManagerFactoryBean multiEntityManager() {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

        factory.setDataSource(dataSource());
        factory.setPackagesToScan(dynamicDataSource.getEntityScan());
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        return factory;
    }

    @Primary
    @Bean(name = "multiTransactionManager")
    public PlatformTransactionManager multiTransactionManager() {
        return new JpaTransactionManager(Objects.requireNonNull(multiEntityManager().getObject()));
    }

    @Bean
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcDaoSupport namedParameterJdbcDaoSupport(@Qualifier("dataSource") DataSource dataSource) {
        NamedParameterJdbcDaoSupport dao = new NamedParameterJdbcDaoSupport();
        dao.setDataSource(dataSource);
        return dao;
    }
}
