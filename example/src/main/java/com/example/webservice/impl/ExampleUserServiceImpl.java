package com.example.webservice.impl;

import com.example.webservice.dto.UserRequest;
import com.zipe.model.User;
import com.example.webservice.ExampleUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.jws.WebService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@WebService(serviceName = "ExampleUserService",//對外發布的服務名
        targetNamespace = "http://service.exmaple.com/",//指定你想要的名稱空間，通常使用使用包名反轉
        endpointInterface = "com.example.webservice.ExampleUserService")//服務接口全路徑, 指定做SEI（Service EndPoint Interface）服務端點接口
@Component
public class ExampleUserServiceImpl implements ExampleUserService {

    private Map<String, User> userMap = new HashMap<>();

    public ExampleUserServiceImpl() {
        log.info("向實體類插入數據");
        User user = new User();
        user.setUserId(UUID.randomUUID().toString().replace("-", ""));
        user.setUserName("mracale01");
        user.setEmail("mracale01@163.xom");
        userMap.put(user.getUserName(), user);

        user = new User();
        user.setUserId(UUID.randomUUID().toString().replace("-", ""));
        user.setUserName("mracale02");
        user.setEmail("mracale02@163.xom");
        userMap.put(user.getUserName(), user);

        user = new User();
        user.setUserId(UUID.randomUUID().toString().replace("-", ""));
        user.setUserName("mracale03");
        user.setEmail("mracale03@163.xom");
        userMap.put(user.getUserName(), user);
    }

    @Override
    public String getUserId(String name) {
        log.info("userName:{}", name);
        return "userId為：" + userMap.get(name).getUserId();
    }

    @Override
    public Map<String, User> getAllUserData() {
        return userMap;
    }

    @Override
    public User getUser(UserRequest userRequest) {
        log.info("userMap是:{}", userMap);
        return userMap.get(userRequest.getName());
    }

}
