package com.zipe.quartz.autoconfiguration;

import com.zaxxer.hikari.HikariDataSource;
import com.zipe.quartz.config.QuartzDataSourceProperties;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.quartz.autoconfigure.QuartzDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.StringUtils;

/**
 * Quartz 資料來源自動配置類別。
 * <p>
 * 當 {@code spring.quartz.enable=true} 且 {@code spring.quartz.job-store-type=jdbc} 時，
 * 自動讀取 {@code spring.datasource.quartz.*} 與 {@code spring.datasource.hikari.*} 設定，
 * 建立 HikariCP 連線池並以 {@link org.springframework.boot.quartz.autoconfigure.QuartzDataSource}
 * 標記供 Quartz 排程框架專用，避免與應用程式主要資料來源混用。
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

    /** Quartz 資料來源相關連線設定，由 Spring 自動注入。 */
    private final QuartzDataSourceProperties quartzDataSourceProperties;

    /**
     * 建構子注入 {@link QuartzDataSourceProperties}。
     * <p>
     * 採用建構子注入而非欄位注入，確保物件在初始化時即具備完整的資料來源設定。
     *
     * @param quartzDataSourceProperties Quartz 資料來源屬性設定物件
     */
    DataSourceAutoConfiguration(QuartzDataSourceProperties quartzDataSourceProperties) {
        this.quartzDataSourceProperties = quartzDataSourceProperties;
    }

    /**
     * 根據 {@link DataSourceProperties} 建立 {@link HikariDataSource} 實例。
     * <p>
     * 若 {@code properties.getName()} 有值，則一併設定連線池名稱，便於監控與日誌識別。
     *
     * @param properties 包含 JDBC 連線資訊（URL、帳號、密碼、驅動）的資料來源屬性物件
     * @return 已初始化的 {@link HikariDataSource} 連線池實例
     */
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
     * 建立並註冊 Quartz 專用的 {@link DataSource} Bean。
     * <p>
     * 從 {@link QuartzDataSourceProperties}（對應 {@code spring.datasource.quartz.*}）
     * 讀取連線設定，再套用 {@code spring.datasource.hikari.*} 的 HikariCP 連線池參數，
     * 最終產生一個標記為 {@code @QuartzDataSource} 的連線池，使 Quartz 排程框架
     * 使用此專屬資料來源而非應用程式的主要資料來源。
     *
     * @return 供 Quartz 排程使用的 {@link HikariDataSource} 連線池實例
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
