package com.zipe.autoconfiguration;

import com.zipe.config.WebServicePropertyConfig;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * CXF WebService 自動配置類別。
 *
 * <p>當 {@link WebServicePropertyConfig} 存在於 classpath 時自動生效，
 * 負責向 Spring Boot 內嵌容器註冊 {@link CXFServlet}，
 * 並依據 {@code webservice.uri-mapping} 屬性設定 Servlet 的 URL 路由前綴，
 * 使所有 CXF 服務端點可透過指定路徑對外公開。
 *
 * @author : Gary Tsai
 **/
@AutoConfiguration
@ConditionalOnClass(WebServicePropertyConfig.class)
@EnableConfigurationProperties(WebServicePropertyConfig.class)
public class CxfConfigAutoConfiguration {

    /** WebService 相關屬性設定，包含 URI 映射路徑等配置值。 */
    private final WebServicePropertyConfig webServicePropertyConfig;

    /**
     * 建構子，透過 Spring 依賴注入取得 WebService 屬性設定物件。
     *
     * @param webServicePropertyConfig WebService 屬性設定，由 Spring 自動注入
     */
    @Autowired
    public CxfConfigAutoConfiguration(WebServicePropertyConfig webServicePropertyConfig) {
        this.webServicePropertyConfig = webServicePropertyConfig;
    }

    /**
     * 向 Spring Boot 內嵌容器註冊 {@link CXFServlet}。
     *
     * <p>使用 {@link WebServicePropertyConfig#getUriMapping()} 所設定的路徑作為
     * Servlet 的 URL 映射，讓所有 CXF WebService 端點皆由此 Servlet 統一處理。
     *
     * @return 包含 {@link CXFServlet} 及其 URL 映射的 {@link ServletRegistrationBean}
     */
    @Bean
    public ServletRegistrationBean cxfServletRegistration() {
        // 以設定檔中的 URI 映射路徑（如 /services/*）綁定 CXFServlet
        return new ServletRegistrationBean(new CXFServlet(), webServicePropertyConfig.getUriMapping());
    }

}
