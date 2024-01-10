package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 10:11
 **/
@Configuration
@ConfigurationProperties(prefix = "web.thymeleaf")
@Data
public class ThymeleafConfig {

    /**
     * Thymeleaf 啟動開關
     * Default=false
     */
    private Boolean enable = true;
    /**
     * Thymeleaf 檔案目錄
     * Default=html/*,vue/*,templates/*,th/*
     */
    private String viewNames = "html/*,vue/*,templates/*,th/*";
    /**
     * Thymeleaf 副檔名
     * Default=.html
     */
    private String stuff = ".html";
    /**
     * Template model
     * Default=HTML
     */
    private String templateMode = "HTML";

}
