package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Web 模組的頂層屬性設定類別。
 *
 * <p>對應 {@code application.yml} 中以 {@code web} 為前綴的設定區塊，
 * 並將子區塊分別委派給 {@link WebResourceConfig}、{@link JspConfig} 與
 * {@link ThymeleafConfig} 三個巢狀設定物件。</p>
 *
 * <p>使用範例：</p>
 * <pre>{@code
 * web:
 *   resource:
 *     ...
 *   jsp:
 *     ...
 *   thymeleaf:
 *     ...
 * }</pre>
 *
 * @author : Gary Tsai
 **/
@Configuration
@ConfigurationProperties(prefix = "web")
@Data
public class WebPropertyConfig {

    /**
     * 網頁資源設定
     */
    private WebResourceConfig resource = new WebResourceConfig();
    /**
     * Jsp 進階設定
     */
    private JspConfig jsp = new JspConfig();
    /**
     * Thymeleaf 進階設定
     */
    private ThymeleafConfig thymeleaf = new ThymeleafConfig();
}
