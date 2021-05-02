package com.zipe.util;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

/**
 * Web service
 * @author zipe
 */
public class CxfClientUtil<T> {

    private String clientUrl;
    private Class targetClass;

    public CxfClientUtil(String clientUrl, Class targetClass) {
        this.clientUrl = clientUrl;
        this.targetClass = targetClass;
    }

    public T callService() {
        // JaxWsProxyFactoryBean调用服务端
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        // 设置服务接口
        jaxWsProxyFactoryBean.setServiceClass(targetClass);
        // 设置服务地址
        jaxWsProxyFactoryBean.setAddress(clientUrl);

        // 获取服务接口实例
        return (T) jaxWsProxyFactoryBean.create(targetClass);
    }
}
