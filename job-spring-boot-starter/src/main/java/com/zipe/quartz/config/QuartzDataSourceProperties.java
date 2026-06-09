package com.zipe.quartz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 排程專用的資料來源設定屬性類別。
 *
 * <p>對應 {@code application.yml} 中 {@code spring.datasource.quartz} 前綴的設定區塊，
 * 讓 Quartz 可使用獨立的資料庫連線，與應用程式主資料來源分開管理。</p>
 *
 * <p>設定範例：
 * <pre>{@code
 * spring:
 *   datasource:
 *     quartz:
 *       url: jdbc:mysql://localhost:3306/quartz_db
 *       username: quartz_user
 *       password: secret
 *       driver-class-name: com.mysql.cj.jdbc.Driver
 * }</pre>
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.datasource.quartz")
public class QuartzDataSourceProperties {
    /** 資料庫連線使用者名稱。 */
    public String username;
    /** 資料庫連線密碼。 */
    public String password;
    /** JDBC 連線 URL，例如 {@code jdbc:mysql://localhost:3306/quartz_db}。 */
    public String url;
    /** JDBC 驅動程式完整類別名稱，例如 {@code com.mysql.cj.jdbc.Driver}。 */
    public String driverClassName;
}
