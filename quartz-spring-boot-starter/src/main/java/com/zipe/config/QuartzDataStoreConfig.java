package com.zipe.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zipe.util.crypto.Base64Util;
import com.zipe.util.crypto.CryptoUtil;
import com.zipe.util.string.StringConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories({ "com.zipe.repository.quartz" })
public class QuartzDataStoreConfig {

	@Autowired
	private Environment env;

	@Bean
	public DataSource quartzDataSource() {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setDriverClassName(env.getProperty("connection.driver_class"));
		hikariConfig.setJdbcUrl(env.getProperty("connection.url"));
		hikariConfig.setUsername(env.getProperty("connection.username"));

		String dbPassword = env.getProperty("connection.pa55word");
		// 如設定檔將編碼加密開啟，需解碼
		String encrypt = env.getProperty("encrypt.enabled");
		if (StringUtils.isNotBlank(encrypt) && encrypt.equalsIgnoreCase(StringConstant.TRUE) && StringUtils.isNotBlank(dbPassword)) {
			CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
			dbPassword = cryptoUtil.decode(dbPassword);
		}
		hikariConfig.setPassword(dbPassword);
		hikariConfig.setMaximumPoolSize(Integer.valueOf(env.getProperty("hikari.maximum.connection.count")));
		hikariConfig.setMinimumIdle(Integer.valueOf(env.getProperty("hikari.minimum.connection.count")));
		hikariConfig.setConnectionTestQuery(env.getProperty("hikari.test.query"));
		hikariConfig.setPoolName(env.getProperty("hikari.pool.name"));

		hikariConfig.addDataSourceProperty("dataSource.cachePrepStmts", env.getProperty("dataSource.cachePrepStmts"));
		hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSize",
				env.getProperty("dataSource.prepStmtCacheSize"));
		hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit",
				env.getProperty("dataSource.prepStmtCacheSqlLimit"));
		hikariConfig.addDataSourceProperty("dataSource.useServerPrepStmts",
				env.getProperty("dataSource.useServerPrepStmts"));

		HikariDataSource dataSource = new HikariDataSource(hikariConfig);

		return dataSource;
	}

	@Bean
	public PlatformTransactionManager quartztTransactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory);
		return txManager;
	}

	@Bean
	public JdbcTemplate quartzJdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	public NamedParameterJdbcDaoSupport quartznNamedParameterJdbcDaoSupport (DataSource dataSource) {
        NamedParameterJdbcDaoSupport dao = new NamedParameterJdbcDaoSupport();
        dao.setDataSource(dataSource);
	    return dao;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean quartzEntityManagerFactory() {
		LocalContainerEntityManagerFactoryBean lcemfb = new LocalContainerEntityManagerFactoryBean();
		lcemfb.setJpaVendorAdapter(getJpaVendorAdapter());
		lcemfb.setDataSource(quartzDataSource());
		lcemfb.setPersistenceUnitName("myJpaPersistenceUnit");
		lcemfb.setPackagesToScan("com.ebizprise.winw.project.entity");
		lcemfb.setJpaProperties(jpaProperties());
		lcemfb.setSharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE);
		return lcemfb;
	}

	public JpaVendorAdapter getJpaVendorAdapter() {
		JpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		return adapter;
	}

	private Properties jpaProperties() {
		Properties props = new Properties();
		props.put("hibernate.dialect", env.getProperty("hibernate.dialect"));
		props.put("hibernate.query.substitutions", env.getProperty("hibernate.query.substitutions"));
		props.put("hibernate.default_batch_fetch_size", env.getProperty("hibernate.default_batch_fetch_size"));
		props.put("hibernate.max_fetch_depth", env.getProperty("hibernate.max_fetch_depth"));
		props.put("hibernate.bytecode.use_reflection_optimizer",
				env.getProperty("hibernate.bytecode.use_reflection_optimizer"));
		props.put("hibernate.cache.use_second_level_cache", env.getProperty("hibernate.cache.use_second_level_cache"));
		props.put("hibernate.cache.use_query_cache", env.getProperty("hibernate.cache.use_query_cache"));
		props.put("hibernate.cache.region.factory_class", env.getProperty("hibernate.cache.region.factory_class"));
		props.put("hibernate.show_sql", env.getProperty("hibernate.show_sql"));
//		props.put("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto"));
		props.put("hibernate.cache.provider_configuration", env.getProperty("hibernate.cache.provider_configuration"));
		return props;
	}
}
