package com.zipe.service.impl;

import com.zipe.model.User;
import com.zipe.service.UserService;
import jakarta.jws.WebService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @ClassName:UserServiceImpl
 */
@WebService(serviceName = "UserService",//對外發布的服務名
        targetNamespace = "http://service.zipe.com",//指定你想要的名稱空間，通常使用使用包名反轉
        endpointInterface = "com.zipe.service.UserService")//服務接口全路徑, 指定做SEI（Service EndPoint Interface）服務端點接口
@Component
public class UserServiceImpl implements UserService {

    private final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final Map<String, User> userMap = new HashMap<>();

    public UserServiceImpl() {
        logger.info("向實體類插入數據");
        User user = new User();
        user.setUserId(UUID.randomUUID().toString().replace("-", ""));
        user.setUserName("mracale01");
        user.setEmail("mracale01@163.xom");
        userMap.put(user.getUserId(), user);

        user = new User();
        user.setUserId(UUID.randomUUID().toString().replace("-", ""));
        user.setUserName("mracale02");
        user.setEmail("mracale02@163.xom");
        userMap.put(user.getUserId(), user);

        user = new User();
        user.setUserId(UUID.randomUUID().toString().replace("-", ""));
        user.setUserName("mracale03");
        user.setEmail("mracale03@163.xom");
        userMap.put(user.getUserId(), user);
    }

    @Override
    public String getUserName(String userId) {
        return "userId為：" + userId;
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
