package com.zipe.quartz.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.quartz.job-store-type", havingValue = "jdbc")
public class DataSourceConfiguration {

    private static HikariDataSource createHikariDataSource(DataSourceProperties properties) {
        // 建立 HikariDataSource 物件
        HikariDataSource dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        // 設定執行緒池名
        if (StringUtils.hasText(properties.getName())) {
            dataSource.setPoolName(properties.getName());
        }
        return dataSource;
    }

    /**
     * 讀取 spring.datasource.quartz 設定到 DataSourceProperties 物件
     *
     * 建立 quartz 資料來源的設定物件
     */
    @Primary
    @Bean(name = "quartzDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.quartz")
    public DataSourceProperties quartzDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 建立 quartz 資料來源
     */
    @Bean(name = "quartzDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    @QuartzDataSource
    public DataSource quartzDataSource() {
        // 獲得 DataSourceProperties 物件
        DataSourceProperties properties = this.quartzDataSourceProperties();
        // 建立 HikariDataSource 物件
        return createHikariDataSource(properties);
    }

}
