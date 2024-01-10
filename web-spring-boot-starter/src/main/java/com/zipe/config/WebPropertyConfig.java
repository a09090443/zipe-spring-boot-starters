package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
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
