package com.zipe.config;

import com.zipe.model.Shanhy;
import com.zipe.model.ShanhyA;
import com.zipe.model.ShanhyB;
import com.zipe.service.impl.UserServiceImpl;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

@Configuration
public class MyBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(MyBeanDefinitionRegistryPostProcessor.class);
    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
    private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

    private ApplicationContext applicationContext;
    private WebServicePropertyConfig webServicePropertyConfig;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        logger.info("Invoke Metho postProcessBeanFactory");
//        webServicePropertyConfig.getDownstreamServices().getRest().forEach((beanName, props) -> makeClient(beanName, props, registry));
        BeanDefinition bd = beanFactory.getBeanDefinition("shanhyA");
        MutablePropertyValues mpv = bd.getPropertyValues();
        mpv.addPropertyValue("test", "123456788");

        System.out.println(webServicePropertyConfig);
//        webServicePropertyConfig.getMap().forEach((k, v) -> {
//            System.out.println(k);
//            Bus bus = applicationContext.getBean("bus", SpringBus.class);
//            EndpointImpl endpoint =
//                    new EndpointImpl(bus, new UserServiceImpl());
//            endpoint.publish("/user");
//
//        });
// 這裡可以設定屬性，例如
//        BeanDefinition bd = beanFactory.getBeanDefinition("dataSourceA");
//        MutablePropertyValues mpv = bd.getPropertyValues();
//        mpv.addPropertyValue("driverClassName", "com.mysql.jdbc.Driver");
//        mpv.addPropertyValue("url", "jdbc:mysql://localhost:3306/test");
//        mpv.addPropertyValue("username", "root");
//        mpv.addPropertyValue("password", "123456");
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        logger.info("Invoke Metho postProcessBeanDefinitionRegistry");
//        ShanhyA test = new ShanhyA();
//        test.setTest("aaaaaaaaaa");
        registerBean(registry, "shanhyA", ShanhyA.class);
        registerBean(registry, "shanhyB", ShanhyB.class);
//        registerBean(registry, "bus", SpringBus.class);
    }

    private void registerBean(BeanDefinitionRegistry registry, String name, Class<?> beanClass) {
        AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
        ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
        abd.setScope(scopeMetadata.getScopeName());
// 可以自動生成name
        String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, registry));
        AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
        BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
    }

    @Override
    public void setEnvironment(@Nullable Environment environment) {
        bindProperties(environment);
    }

    private void bindProperties(Environment environment) {
        this.webServicePropertyConfig = Binder.get(environment)
                .bind("web.service", WebServicePropertyConfig.class)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
