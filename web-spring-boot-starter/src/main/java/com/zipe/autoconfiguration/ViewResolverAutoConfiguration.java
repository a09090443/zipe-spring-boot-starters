package com.zipe.autoconfiguration;

import com.zipe.config.WebPropertyConfig;
import com.zipe.util.string.StringConstant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Locale;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/21 上午 11:38
 **/
@Configuration
@ConditionalOnClass(WebPropertyConfig.class)
@EnableConfigurationProperties(WebPropertyConfig.class)
public class ViewResolverAutoConfiguration extends WebMvcConfigurationSupport {
    private final String WEB_BASE_DIR = "/WEB-INF/";

    private final WebPropertyConfig webPropertyConfig;

    ViewResolverAutoConfiguration(WebPropertyConfig webPropertyConfig) {
        this.webPropertyConfig = webPropertyConfig;
    }

    @Bean
    @ConditionalOnProperty(name = "web.jsp.enable", havingValue = "true")
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
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
        templateResolver.setTemplateMode(TemplateMode.HTML);
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
        return (factory) -> factory.setRegisterDefaultServlet(true);
    }

    /**
     * 默認解析器 其中locale表示默認語言
     *
     * @return
     */
    @Bean
    @Override
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(Locale.TAIWAN);
        resolver.setCookieName("localeCookie");
        resolver.setCookieMaxAge(4800);
        return resolver;
    }

}
