package com.zipe;

import com.zipe.model.User;
import com.zipe.service.UserService;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import java.util.Map;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/5/4 下午 02:55
 **/
public class ClientTest {
    public static void main(String[] args) {
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

        // 創建動態客戶端
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        Client client = dcf.createClient("http://127.0.0.1:8080/webservice/user?wsdl");
        // 需要密碼的情況需要加上用戶名和密碼
        // client.getOutInterceptors().add(new ClientLoginInterceptor(USER_NAME, PASS_WORD));
        Object[] objects = new Object[0];
        try {
            // invoke("方法名",參數1,參數2,參數3....);
            objects = client.invoke("getUserName", "test");
            System.out.println("返回數據:" + objects[0]);
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}
