package com.zipe.autoconfiguration;

import com.zipe.config.WebServicePropertyConfig;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/5/4 下午 02:09
 **/
@Configuration
@ConditionalOnClass(WebServicePropertyConfig.class)
@EnableConfigurationProperties(WebServicePropertyConfig.class)
public class CxfConfigAutoConfiguration {

    private final WebServicePropertyConfig webServicePropertyConfig;

    public CxfConfigAutoConfiguration(WebServicePropertyConfig webServicePropertyConfig) {
        this.webServicePropertyConfig = webServicePropertyConfig;
    }

    @Bean
    public ServletRegistrationBean cxfServlet() {
        return new ServletRegistrationBean(new CXFServlet(), webServicePropertyConfig.getUriMapping());
    }

}
