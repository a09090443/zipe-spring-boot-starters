package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Velocity 樣板引擎的外部化設定類別。
 *
 * <p>透過 {@code @ConfigurationProperties(prefix = "velocity")} 將
 * {@code application.yml} 或 {@code application.properties} 中以
 * {@code velocity.*} 為前綴的屬性自動繫結至本類別的欄位。</p>
 *
 * <p>使用方式範例（application.yml）：</p>
 * <pre>
 * velocity:
 *   dir-path: templates/velocity
 * </pre>
 *
 * @author : Gary Tsai
 **/
@Configuration
@ConfigurationProperties(prefix = "velocity")
@Data
public class VelocityPropertyConfig {
    /**
     * Velocity 樣板檔案的根目錄路徑。
     * <p>對應設定鍵：{@code velocity.dir-path}，預設值為 {@code template}。</p>
     * Default=template
     */
    private String dirPath = "template";
}
