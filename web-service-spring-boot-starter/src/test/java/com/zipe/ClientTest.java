package com.zipe;

import com.zipe.util.WebServiceClientUtil;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/5/4 下午 02:55
 **/
public class ClientTest {
    public static void main(String[] args) throws Exception {
//        try {
//            // 接口地址
//            String address = "http://127.0.0.1:8080/webservice/user?wsdl";
//            // 代理工廠
//            JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
//            // 設置代理地址
//            jaxWsProxyFactoryBean.setAddress(address);
//            // 設置接口類型
//            jaxWsProxyFactoryBean.setServiceClass(UserService.class);
//            // 創建一個代理接口實現
//            UserService us = (UserService) jaxWsProxyFactoryBean.create();
//            // 數據准備
////            String userId = "maple";
//            // 調用代理接口的方法調用並返回結果
////            String result = us.getUserName(userId);
//            Map<String, User> userMaps= us.getAllUserData();
//            System.out.println("返回結果:" + userMaps);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        WebServiceClientUtil clientUtil = new WebServiceClientUtil("http://127.0.0.1:8080/webservice/helloWorld?wsdl",
                "getAllUserData",new Object[]{});
        Object[] test = clientUtil.invoke();
        System.out.println(test);
    }
}
