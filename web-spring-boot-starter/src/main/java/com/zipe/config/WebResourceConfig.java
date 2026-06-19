package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 靜態資源路由設定類別。
 *
 * <p>讀取 {@code web.resource} 前綴的設定屬性，定義靜態資源的
 * URL 路徑模式（{@code pathPattern}）與實際存放目錄（{@code location}），
 * 供 {@link com.zipe.autoconfiguration.WebAutoConfiguration} 或
 * Spring MVC 的資源處理器（ResourceHandler）使用。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 11:25
 **/
@Configuration
@ConfigurationProperties(prefix="web.resource")
@Data
public class WebResourceConfig {

    /**
     * 靜態資源位置 Uri
     * Default=/static/**
     */
    private String pathPattern = "/static/**";
    /**
     * 靜態資源目錄
     * Default=/WEB-INF/static/
     */
    private String location = "/WEB-INF/static/";

}
