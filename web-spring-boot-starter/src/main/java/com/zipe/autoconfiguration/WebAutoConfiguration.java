package com.zipe.autoconfiguration;

import com.zipe.config.WebPropertyConfig;
import com.zipe.util.DateFormatter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Web MVC 自動配置類別。
 *
 * <p>當 {@link WebPropertyConfig} 類別存在於 classpath 時自動啟用，
 * 負責設定靜態資源映射、語系切換攔截器、預設 Servlet 處理，
 * 以及日期格式化轉換器，以統一 Web 層的基礎行為。</p>
 *
 * @author : Gary Tsai
 **/
@AutoConfiguration
@ConditionalOnClass(WebPropertyConfig.class)
@EnableConfigurationProperties(WebPropertyConfig.class)
public class WebAutoConfiguration implements WebMvcConfigurer {

    /** Web 相關設定屬性，由 application.yml 繫結而來 */
    private final WebPropertyConfig webPropertyConfig;

    /**
     * 建構子，透過 Spring 依賴注入取得 Web 屬性設定。
     *
     * @param webPropertyConfig Web 屬性設定物件
     */
    WebAutoConfiguration(WebPropertyConfig webPropertyConfig) {
        this.webPropertyConfig = webPropertyConfig;
    }

    /**
     * Configure ResourceHandlers to serve static resources like CSS/ Javascript
     * etc...
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 依設定檔中定義的路徑模式與實際資源位置，註冊靜態資源處理器
        registry.addResourceHandler(webPropertyConfig.getResource().getPathPattern())
                .addResourceLocations(webPropertyConfig.getResource().getLocation());
    }

    /**
     * 註冊語系切換攔截器，使應用程式可依請求參數動態切換語系。
     *
     * @param registry Spring MVC 攔截器註冊表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    /**
     * 啟用預設 Servlet 處理，讓容器的 DefaultServlet 能夠處理靜態資源請求。
     *
     * @param configurer 預設 Servlet 處理設定物件
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    /**
     * 註冊自訂的日期格式化轉換器，使 Spring MVC 能正確解析請求中的日期字串。
     *
     * @param registry Spring MVC 格式化轉換器註冊表
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new DateFormatter());
    }

    /**
     * 建立並設定語系切換攔截器。
     *
     * <p>攔截 HTTP 請求中的 {@code language} 參數，
     * 讓前端可透過傳遞該參數即時切換應用程式的顯示語系。</p>
     *
     * @return 已設定 paramName 為 {@code "language"} 的 {@link LocaleChangeInterceptor}
     */
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        // 指定觸發語系切換的請求參數名稱
        localeChangeInterceptor.setParamName("language");
        return localeChangeInterceptor;
    }

}
