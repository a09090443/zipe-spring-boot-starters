package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JSP 視圖引擎設定屬性類別。
 *
 * <p>對應 {@code application.yml} 中 {@code web.jsp} 前綴的設定項目，
 * 控制 JSP 視圖解析器的啟用狀態、視圖名稱匹配規則與副檔名。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 10:09
 **/
@Configuration
@ConfigurationProperties(prefix = "web.jsp")
@Data
public class JspConfig {

    /**
     * Jsp 啟動開關
     * Default=false
     */
    private Boolean enable = false;
    /**
     * Jsp 檔案目錄
     * Default=jsp/*
     */
    private String viewNames = "jsp/*";
    /**
     * Jsp 副檔名
     * Default=.jsp
     */
    private String stuff = ".jsp";

}
