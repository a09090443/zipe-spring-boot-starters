package com.zipe.autoconfiguration;

import com.zipe.config.WebServicePropertyConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 * @author Gary Tsai
 */
@Slf4j
@Configuration
@ConditionalOnClass(WebServicePropertyConfig.class)
@EnableConfigurationProperties(WebServicePropertyConfig.class)
public class WebServiceRegisterAutoConfiguration implements InitializingBean, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final Bus bus;

    private final WebServicePropertyConfig webServicePropertyConfig;

    @Autowired
    public WebServiceRegisterAutoConfiguration(Bus bus, WebServicePropertyConfig webServicePropertyConfig) {
        this.bus = bus;
        this.webServicePropertyConfig = webServicePropertyConfig;
    }

    @Override
    public void afterPropertiesSet() {

        webServicePropertyConfig.getMap().forEach((k, v) -> {
            EndpointImpl endpoint;
            try {
                endpoint = new EndpointImpl(bus, applicationContext.getBean(v.getBeanName()));
                endpoint.publish(v.getUriMapping());
                log.info("Web Service 註冊服務:{}, 對應 URI:{}", v.getBeanName(), v.getUriMapping());
            } catch (Exception e) {
                log.error("Web Service 註冊服務:{}, 失敗", v.getBeanName());
                throw e;
            }

        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
