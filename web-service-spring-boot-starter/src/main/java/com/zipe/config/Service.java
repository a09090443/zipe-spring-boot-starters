package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WebService 單一服務的設定屬性類別。
 *
 * <p>對應 {@code application.yml} 中 {@code web.service.map} 前綴的屬性，
 * 用於描述每一筆 WebService 服務的 Spring Bean 名稱與 URI 路徑對應。
 * 通常作為集合元素由 {@code WebServicePropertyConfig} 統一管理。</p>
 *
 * @author Gary Tsai
 */
@Configuration
@ConfigurationProperties(prefix="web.service.map")
@Data
public class Service {
    /**
     * 實作 WebService 介面的 Spring Bean 名稱。
     * 必須與業務專案中 {@code @Component} 的 value 完全一致，
     * 供 {@code ApplicationContext.getBean(beanName)} 取出服務實作物件。
     */
    private String beanName;
    /**
     * 此端點在 CXF Servlet 下的相對路徑（例如 {@code /user}）。
     * 最終 WSDL URL 由 {@code WebServicePropertyConfig.uriMapping}（去除萬用字元）
     * 串接此路徑後加上 {@code ?wsdl} 組成。
     */
    private String uriMapping;
}
