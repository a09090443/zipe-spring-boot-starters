package com.zipe.config;

import com.zipe.service.UserService;
import com.zipe.service.impl.UserServiceImpl;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;

@Configuration
public class WebServiceRegister implements InitializingBean, ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Autowired
    private Bus bus;

    @Autowired
    private WebServicePropertyConfig webServicePropertyConfig;

    @Override
    public void afterPropertiesSet() throws Exception {

        System.out.println(bus);
        System.out.println(webServicePropertyConfig);
//        UserService userService = (UserServiceImpl) applicationContext.getBean("userService");
        webServicePropertyConfig.getMap().forEach((k, v) -> {
            System.out.println(k);
            EndpointImpl endpoint =
                    null;
            try {
                endpoint = new EndpointImpl(bus, Class.forName("com.zipe.service.impl.UserServiceImpl").getDeclaredConstructor().newInstance());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            endpoint.publish("/user");

        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
