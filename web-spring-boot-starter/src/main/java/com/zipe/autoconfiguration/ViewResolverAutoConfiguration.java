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
 * @author : Gary Tsai
 **/
@AutoConfiguration
@ConditionalOnClass(WebPropertyConfig.class)
@EnableConfigurationProperties(WebPropertyConfig.class)
public class ViewResolverAutoConfiguration {

    private final String WEB_BASE_DIR = "/WEB-INF/";

    private final WebPropertyConfig webPropertyConfig;

    WebApplicationContext webApplicationContext;

    ViewResolverAutoConfiguration(WebPropertyConfig webPropertyConfig, WebApplicationContext webApplicationContext) {
        this.webPropertyConfig = webPropertyConfig;
        this.webApplicationContext = webApplicationContext;
    }

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

    @Bean
    @ConditionalOnBean(name = "templateResolver")
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        return templateEngine;
    }

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

    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> enableDefaultServlet() {
        return factory -> factory.setRegisterDefaultServlet(true);
    }

    /**
     * 默認解析器 其中locale表示默認語言
     *
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("localeCookie");
        resolver.setDefaultLocale(Locale.TAIWAN);
        resolver.setCookieMaxAge(Duration.ofSeconds(4800));
        return resolver;
    }

}
