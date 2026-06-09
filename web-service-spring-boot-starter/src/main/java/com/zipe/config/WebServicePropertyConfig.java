package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * WebService 屬性設定類別。
 *
 * <p>讀取 {@code application.yml} 中 {@code web.service} 前綴的設定，
 * 包含 Servlet URI 對應路徑與各 WebService 服務的 Bean 名稱及路徑對應表。
 * 由 CXF 自動配置類別（{@code CxfConfigAutoConfiguration}）注入使用，
 * 以完成服務端點的動態註冊。</p>
 *
 * @author Gary.Tsai
 */
@Configuration
@ConfigurationProperties(prefix = "web.service")
@Data
public class WebServicePropertyConfig {
    /**
     * CXF Servlet 的 URL 對應前綴（含萬用字元 {@code *}）。
     * 所有透過本 Starter 發布的 SOAP 端點 URL 均以此前綴起頭。
     * 預設值為 {@code /webservice/*}。
     */
    private String uriMapping = "/webservice/*";
    /**
     * 以邏輯名稱（任意字串）為鍵、{@link Service} 設定物件為值的端點對應表。
     * 每筆記錄描述一個 SOAP 服務的 Spring Bean 名稱與 URI 路徑，
     * 由 {@code WebServiceRegisterAutoConfiguration} 在啟動時逐一讀取並發布端點。
     */
    private Map<String, Service> map;
}
