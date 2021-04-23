package com.zipe.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/23 下午 13:44
 **/
public class ApplicationContextHelper implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    @SuppressWarnings("static-access")
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static Object getBean(String beanName) {
        if (applicationContext == null) {
            throw new NullPointerException("ApplicationContext is null!");
        }
        return applicationContext.getBean(beanName);
    }

    /**
     * 根據類獲取bean
     *
     * @param clazz
     * @param <T>
     * @return <T>
     */
    public static <T> T popBean(Class<T> clazz) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(clazz);
    }

    /**
     * 根據類和bean的名字獲取bean
     *
     * @param name
     * @param clazz
     * @param <T>
     * @return <T>
     */
    public static <T> T popBean(String name, Class<T> clazz) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(name, clazz);
    }
}
