package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Thymeleaf 視圖引擎設定屬性類別。
 *
 * <p>透過 {@code web.thymeleaf.*} 前綴從 {@code application.yml} / {@code application.properties}
 * 讀取設定，供 {@code ViewResolverAutoConfiguration} 在啟動時建立 Thymeleaf 視圖解析器使用。</p>
 *
 * <p>範例設定：
 * <pre>
 * web:
 *   thymeleaf:
 *     enable: true
 *     view-names: html/*,th/*
 *     stuff: .html
 *     template-mode: HTML
 * </pre>
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 10:11
 **/
@Configuration
@ConfigurationProperties(prefix = "web.thymeleaf")
@Data
public class ThymeleafConfig {

    /**
     * Thymeleaf 視圖引擎啟動開關。
     * 設為 {@code true} 時才會建立 Thymeleaf 視圖解析器。
     * 預設值：{@code true}
     */
    private Boolean enable = true;
    /**
     * Thymeleaf 可處理的視圖名稱萬用字元清單，以逗號分隔。
     * 符合此規則的視圖名稱才會交由 Thymeleaf 解析，其餘則回退至其他視圖解析器（例如 JSP）。
     * 預設值：{@code html/*,vue/*,templates/*,th/*}
     */
    private String viewNames = "html/*,vue/*,templates/*,th/*";
    /**
     * Thymeleaf 範本檔案的副檔名。
     * 解析視圖時會自動附加此後綴以定位實體檔案。
     * 預設值：{@code .html}
     */
    private String stuff = ".html";
    /**
     * Thymeleaf 範本模式，對應 {@code org.thymeleaf.templatemode.TemplateMode} 中的模式名稱。
     * 常見值：{@code HTML}、{@code XML}、{@code TEXT}。
     * 預設值：{@code HTML}
     */
    private String templateMode = "HTML";

}
