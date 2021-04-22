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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/14 下午 05:52
 **/
@Configuration
@PropertySource({"classpath:data-source.properties"})
@EnableJpaRepositories(
        basePackages = "${spring.jpa.base.packages}",
        entityManagerFactoryRef = "multiEntityManager",
        transactionManagerRef = "multiTransactionManager"
)
@ConditionalOnClass(DataSourcePropertyConfig.class)
@EnableConfigurationProperties(DataSourcePropertyConfig.class)
public class DataSourceConfigAutoConfiguration extends BaseDataSourceConfig {

    DataSourceConfigAutoConfiguration(Environment env, DataSourcePropertyConfig dynamicDataSource) {
        super(env, dynamicDataSource);
    }

    private DataSource createDataSource(DynamicDataSourceConfig dataSource) {
        //資料來源
        baseHikariConfig().setJdbcUrl(dataSource.getUrl());
        //使用者名稱
        baseHikariConfig().setUsername(dataSource.getUsername());
        String dbPassword = dataSource.getPa55word();
        if (dynamicDataSource.getIsEncrypt()) {
            CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
            dbPassword = cryptoUtil.decode(dbPassword);
        }
        //密碼
        baseHikariConfig().setPassword(dbPassword);

        baseHikariConfig().setDriverClassName(dataSource.getDriverClassName());

        return new HikariDataSource(baseHikariConfig());
    }

    private DataSource createAs400DataSource(DynamicDataSourceConfig dataSource) {
        // 因 AS400 使用 Hikari 額外設定時會發生錯誤，所以只有基本設定
        HikariConfig config = new HikariConfig();
        //資料來源
        config.setJdbcUrl(dataSource.getUrl());
        //使用者名稱
        config.setUsername(dataSource.getUsername());
        String dbPassword = dataSource.getPa55word();
        if (dynamicDataSource.getIsEncrypt()) {
            CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
            dbPassword = cryptoUtil.decode(dbPassword);
        }
        //密碼
        config.setPassword(dbPassword);
        config.setDriverClassName(dataSource.getDriverClassName());
        // AS400 語法不能使用 SELECT 1
        config.setConnectionTestQuery("VALUES 1");
        return new HikariDataSource(config);
    }

    @Bean
    public DataSource dataSource() {

        Map<Object, Object> dataSourceMap = new HashMap<>(16);
        if(Objects.isNull(dynamicDataSource.getDataSourceMap())){
            throw new DataSourceLookupFailureException("請設定 data-source.properties 內容");
        }

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
