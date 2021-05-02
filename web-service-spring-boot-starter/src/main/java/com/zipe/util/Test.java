package com.zipe.util;

import com.zipe.model.User;
import com.zipe.service.UserService;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

public class Test {
    public static void main(String[] args) {
//        CxfClientUtil<UserService> cxfClientUtil = new CxfClientUtil("http://localhost:8080/services/user?wsdl", UserService.class);
//        UserService userService = cxfClientUtil.callService();
//        User user = userService.getUser(10001L);
//        System.out.println(user);

        // 创建动态客户端
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        Client client = dcf.createClient("http://localhost:8080/services/user?wsdl");
        // 需要密码的情况需要加上用户名和密码
//        client.getOutInterceptors().add(new LoginInterceptor("root", "admin"));
        try {
            // invoke("方法名",参数1,参数2,参数3....);
            Object[] objects = client.invoke("getUser", "10001L");
            System.out.println("返回数据:" + objects[0]);
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}
