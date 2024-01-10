package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
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
