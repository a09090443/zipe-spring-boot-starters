package com.zipe.autoconfiguration;

import com.zipe.config.WebServicePropertyConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gary Tsai
 */
@Slf4j
@AutoConfiguration
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
                Object implementor = applicationContext.getBean(v.getBeanName());
                endpoint = new EndpointImpl(bus, implementor);

                // Configure to ignore empty namespaces
                Map<String, Object> properties = new HashMap<>();
                properties.put("set-jaxb-validation-event-handler", "false");

                endpoint.setProperties(properties);

                // Set custom JAXBDataBinding
                JAXBDataBinding jaxbDataBinding = new JAXBDataBinding();
                jaxbDataBinding.setUnwrapJAXBElement(true);
                jaxbDataBinding.setMtomEnabled(true);

                // Configure namespace mapping to ignore empty namespaces
                Map<String, String> nsMap = new HashMap<>();
                nsMap.put("", "http://www.w3.org/2001/XMLSchema");
                jaxbDataBinding.setNamespaceMap(nsMap);

                endpoint.setDataBinding(jaxbDataBinding);

                endpoint.publish(v.getUriMapping());
                log.info("Web Service 註冊服務:{}, 對應 URI:{}", v.getBeanName(), v.getUriMapping());
            } catch (Exception e) {
                log.error("Web Service 註冊服務:{}, 失敗", v.getBeanName());
                throw new RuntimeException(e);
            }

        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
