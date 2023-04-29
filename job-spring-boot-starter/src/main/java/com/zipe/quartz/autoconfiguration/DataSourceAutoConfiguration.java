package com.zipe.quartz.autoconfiguration;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
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

/**
 * Quartz data source
 *
 * @author zipe
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(DataSourceProperties.class)
@EnableConfigurationProperties(DataSourceProperties.class)
@PropertySource(value = {"classpath:quartz-datasource.properties"})
@ConditionalOnExpression("${spring.quartz.enable:true} && '${spring.quartz.job-store-type}'.equals('jdbc')")
public class DataSourceAutoConfiguration {

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
     * <p>
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
