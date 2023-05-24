package com.example.webservice.impl;

import com.example.webservice.ExampleWebService;
import com.zipe.model.User;
import jakarta.jws.WebService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Gary.Tsai
 */
@WebService(serviceName = "ExampleWebService",//對外發布的服務名
        targetNamespace = "http://service.example.com",//指定你想要的名稱空間，通常使用使用包名反轉
        endpointInterface = "com.example.webservice.ExampleWebService")//服務接口全路徑, 指定做SEI（Service EndPoint Interface）服務端點接口
@Component
public class ExampleWebServiceImpl implements ExampleWebService {

    private final Logger logger = LoggerFactory.getLogger(ExampleWebServiceImpl.class);
    private final Map<String, User> userMap = new HashMap<>();

    public ExampleWebServiceImpl() {
        logger.info("向實體類插入數據");
        User user = new User();
        user.setUserId("01");
        user.setUserName("mracale01");
        user.setEmail("mracale01@163.xom");
        userMap.put(user.getUserId(), user);

        user = new User();
        user.setUserId("02");
        user.setUserName("mracale02");
        user.setEmail("mracale02@163.xom");
        userMap.put(user.getUserId(), user);

        user = new User();
        user.setUserId("03");
        user.setUserName("mracale03");
        user.setEmail("mracale03@163.xom");
        userMap.put(user.getUserId(), user);
    }

    @Override
    public String getUserName(String userId) {
        logger.info("userId為：{}", userMap.get(userId).getUserName());
        return userMap.get(userId).getUserName();
    }

    @Override
    public Map<String, User> getAllUserData() {
        return userMap;
    }

    @Override
    public User getUser(String userId) {
        logger.info("userMap是:{}", userMap);
        return userMap.get(userId);
    }

}
