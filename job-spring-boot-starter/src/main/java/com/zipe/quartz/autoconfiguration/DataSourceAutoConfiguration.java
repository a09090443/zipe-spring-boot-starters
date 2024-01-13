package com.zipe.quartz.autoconfiguration;

import com.zaxxer.hikari.HikariDataSource;
import com.zipe.quartz.config.QuartzDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Quartz data source
 *
 * @author zipe
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(QuartzDataSourceProperties.class)
@EnableConfigurationProperties(QuartzDataSourceProperties.class)
@PropertySource(value = {"classpath:quartz-datasource.properties"})
@ConditionalOnExpression("${spring.quartz.enable:true} && '${spring.quartz.job-store-type}'.equals('jdbc')")
public class DataSourceAutoConfiguration {

    private final QuartzDataSourceProperties quartzDataSourceProperties;

    DataSourceAutoConfiguration(QuartzDataSourceProperties quartzDataSourceProperties) {
        this.quartzDataSourceProperties = quartzDataSourceProperties;
    }

    private static HikariDataSource createHikariDataSource(DataSourceProperties properties) {
        // 建立 HikariDataSource 物件
        HikariDataSource dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        // 設定執行緒池名
        if (StringUtils.hasText(properties.getName())) {
            dataSource.setPoolName(properties.getName());
        }
        return dataSource;
    }

//    /**
//     * 讀取 spring.datasource.quartz 設定到 DataSourceProperties 物件
//     * <p>
//     * 建立 quartz 資料來源的設定物件
//     */
//    @Bean(name = "quartzDataSourceProperties")
////    @ConfigurationProperties(prefix = "spring.datasource.quartz")
//    public QuartzDataSourceProperties quartzDataSourceProperties() {
//        System.out.println(quartzDataSourceProperties);
//        new QuartzDataSourceProperties()
//        return new QuartzDataSourceProperties();
//    }

    /**
     * 建立 quartz 資料來源
     */
    @Primary
    @Bean(name = "quartzDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    @QuartzDataSource
    public DataSource quartzDataSource() {
        // 獲得 DataSourceProperties 物件
        DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setDriverClassName(quartzDataSourceProperties.getDriverClassName());
        dataSourceProperties.setUsername(quartzDataSourceProperties.getUsername());
        dataSourceProperties.setPassword(quartzDataSourceProperties.getPassword());
        dataSourceProperties.setUrl(quartzDataSourceProperties.getUrl());
        // 建立 HikariDataSource 物件
        return createHikariDataSource(dataSourceProperties);
    }

}
