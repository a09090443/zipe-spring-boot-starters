package com.zipe.autoconfiguration;

import com.zipe.config.WebPropertyConfig;
import com.zipe.util.string.StringConstant;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.time.Duration;
import java.util.Locale;

/**
 * 視圖解析器自動配置類別。
 *
 * <p>依據 {@code application.yml} 中的設定，條件式地建立以下 Bean：
 * <ul>
 *   <li>JSP 視圖解析器（{@link InternalResourceViewResolver}），當 {@code web.jsp.enable=true} 時啟用</li>
 *   <li>Thymeleaf 範本解析器、範本引擎與視圖解析器，當 {@code web.thymeleaf.enable=true} 時啟用</li>
 *   <li>預設語系解析器（{@link CookieLocaleResolver}），固定以台灣地區為預設語系</li>
 *   <li>啟用 Servlet 容器預設 Servlet，以支援靜態資源的直接存取</li>
 * </ul>
 *
 * @author : Gary Tsai
 **/
@AutoConfiguration
@ConditionalOnClass(WebPropertyConfig.class)
@EnableConfigurationProperties(WebPropertyConfig.class)
public class ViewResolverAutoConfiguration {

    /** WEB-INF 目錄的根路徑，做為 JSP 與 Thymeleaf 範本的共同前綴。 */
    private final String WEB_BASE_DIR = "/WEB-INF/";

    /** Web 相關設定屬性（JSP、Thymeleaf 等）的設定來源。 */
    private final WebPropertyConfig webPropertyConfig;

    /** Spring Web 應用程式上下文，用於初始化視圖解析器與範本解析器。 */
    WebApplicationContext webApplicationContext;

    /**
     * 建構子：注入 Web 設定屬性與 Web 應用程式上下文。
     *
     * @param webPropertyConfig        Web 設定屬性物件
     * @param webApplicationContext    Spring Web 應用程式上下文
     */
    ViewResolverAutoConfiguration(WebPropertyConfig webPropertyConfig, WebApplicationContext webApplicationContext) {
        this.webPropertyConfig = webPropertyConfig;
        this.webApplicationContext = webApplicationContext;
    }

    /**
     * 建立 JSP 視圖解析器 Bean（{@link InternalResourceViewResolver}）。
     *
     * <p>僅在 {@code web.jsp.enable=true} 時注冊。解析器優先順序（order）固定為 1，
     * prefix 固定為 {@code /WEB-INF/}，suffix 由 {@code web.jsp.stuff} 控制（預設 {@code .jsp}），
     * 僅處理符合 {@code web.jsp.viewNames} glob 模式（預設 {@code jsp/*}）的視圖名稱。</p>
     *
     * @return 已設定前綴、後綴與視圖名稱匹配規則的 {@link InternalResourceViewResolver}
     */
    @Bean
    @ConditionalOnProperty(name = "web.jsp.enable", havingValue = "true")
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setApplicationContext(webApplicationContext);
        //不在此加上jsp檔案所在資料目錄，而在controller回傳值才加上檔案所在目錄
        resolver.setPrefix(WEB_BASE_DIR);
        resolver.setSuffix(webPropertyConfig.getJsp().getStuff());
        resolver.setViewNames(webPropertyConfig.getJsp().getViewNames().split(StringConstant.COMMA));
        resolver.setOrder(1);
        return resolver;
    }

    /**
     * 建立 Thymeleaf 範本解析器 Bean（{@link SpringResourceTemplateResolver}）。
     *
     * <p>僅在 {@code web.thymeleaf.enable=true} 時注冊。prefix 固定為 {@code /WEB-INF/}，
     * suffix 由 {@code web.thymeleaf.stuff} 控制（預設 {@code .html}），
     * 字元編碼固定為 UTF-8，快取（cacheable）設為 {@code false}（適合開發環境；
     * 生產環境請透過 {@code spring.thymeleaf.cache=true} 啟用快取）。</p>
     *
     * @return 已套用設定的 {@link SpringResourceTemplateResolver} 實例
     */
    @Bean
    @ConditionalOnProperty(name = "web.thymeleaf.enable", havingValue = "true")
    public ITemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(webApplicationContext);
        templateResolver.setTemplateMode(webPropertyConfig.getThymeleaf().getTemplateMode());
        //不在此加上jsp檔案所在資料目錄，而在controller回傳值才加上檔案所在目錄
        templateResolver.setPrefix(WEB_BASE_DIR);
        templateResolver.setSuffix(webPropertyConfig.getThymeleaf().getStuff());
        templateResolver.setCharacterEncoding(StringConstant.ENCODE_UTF8);
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    /**
     * 建立 Thymeleaf 範本引擎 Bean（{@link SpringTemplateEngine}）。
     *
     * <p>依賴 {@code templateResolver} Bean 存在才會注冊（{@code @ConditionalOnBean}），
     * 注入已設定好的 {@link ITemplateResolver}，供 {@link ThymeleafViewResolver} 使用。</p>
     *
     * @return 已注入範本解析器的 {@link SpringTemplateEngine} 實例
     */
    @Bean
    @ConditionalOnBean(name = "templateResolver")
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        return templateEngine;
    }

    /**
     * 建立 Thymeleaf 視圖解析器 Bean（{@link ThymeleafViewResolver}）。
     *
     * <p>僅在 {@code web.thymeleaf.enable=true} 時注冊。解析器優先順序（order）固定為 2，
     * 字元編碼固定為 UTF-8，僅處理符合 {@code web.thymeleaf.viewNames} glob 模式
     * （預設 {@code html/*,vue/*,templates/*,th/*}）的視圖名稱。</p>
     *
     * @return 已套用設定的 {@link ThymeleafViewResolver} 實例
     */
    @Bean
    @ConditionalOnProperty(name = "web.thymeleaf.enable", havingValue = "true")
    public ThymeleafViewResolver viewResolverThymeLeaf() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setCharacterEncoding(StringConstant.ENCODE_UTF8);
        viewResolver.setOrder(2);
        viewResolver.setViewNames(webPropertyConfig.getThymeleaf().getViewNames().split(StringConstant.COMMA));
        //網頁檔案存放目錄需符合viewNames中的值才可被成功解析顯示
        return viewResolver;
    }

    /**
     * 啟用 Servlet 容器的 DefaultServlet，使靜態資源可直接透過 URL 存取。
     *
     * <p>JSP 渲染依賴 DefaultServlet 才能正常運作；此 Bean 無條件注冊，
     * 不受 JSP / Thymeleaf 的啟用開關影響。</p>
     *
     * @return 已設定 {@code registerDefaultServlet=true} 的 {@link WebServerFactoryCustomizer}
     */
    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> enableDefaultServlet() {
        return factory -> factory.setRegisterDefaultServlet(true);
    }

    /**
     * 建立基於 Cookie 的語系解析器 Bean（{@link CookieLocaleResolver}）。
     *
     * <p>無條件注冊。Cookie 名稱為 {@code localeCookie}，預設語系為
     * {@link java.util.Locale#TAIWAN}（台灣繁體中文），Cookie 有效期為 4800 秒。
     * 搭配 {@link org.springframework.web.servlet.i18n.LocaleChangeInterceptor}，
     * 前端可透過 URL 參數 {@code language} 即時切換應用程式語系，
     * 切換結果會持久化於 Cookie 中。</p>
     *
     * @return 已設定預設語系與 Cookie 有效期的 {@link CookieLocaleResolver}
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("localeCookie");
        resolver.setDefaultLocale(Locale.TAIWAN);
        resolver.setCookieMaxAge(Duration.ofSeconds(4800));
        return resolver;
    }

}
