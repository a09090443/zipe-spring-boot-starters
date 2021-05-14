package com.zipe.autoconfiguration;

import com.zipe.config.WebServicePropertyConfig;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 **/
@Configuration
@ConditionalOnClass(WebServicePropertyConfig.class)
@EnableConfigurationProperties(WebServicePropertyConfig.class)
public class CxfConfigAutoConfiguration {

    private final WebServicePropertyConfig webServicePropertyConfig;

    @Autowired
    public CxfConfigAutoConfiguration(WebServicePropertyConfig webServicePropertyConfig) {
        this.webServicePropertyConfig = webServicePropertyConfig;
    }

    @Bean
    public ServletRegistrationBean cxfServletRegistration() {
        return new ServletRegistrationBean(new CXFServlet(), webServicePropertyConfig.getUriMapping());
    }

}
