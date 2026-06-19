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
 * {@link ExampleWebService} 的 CXF WebService 實作類別。
 * <p>
 * 使用記憶體內的 {@link HashMap} 作為暫存資料儲存，於建構子初始化三筆測試用使用者資料，
 * 供 WebService 端點進行查詢示範。
 * </p>
 *
 * @author Gary.Tsai
 */
@WebService(serviceName = "ExampleWebService",//對外發布的服務名
        targetNamespace = "http://service.example.com",//指定你想要的命名空間，通常使用套件名稱反轉
        endpointInterface = "com.example.webservice.ExampleWebService")//服務介面全路徑，指定做 SEI（Service EndPoint Interface）服務端點介面
@Component
public class ExampleWebServiceImpl implements ExampleWebService {

    /** SLF4J 日誌記錄器 */
    private final Logger logger = LoggerFactory.getLogger(ExampleWebServiceImpl.class);

    /** 以使用者 ID 為鍵的記憶體使用者資料表，作為本範例的資料來源 */
    private final Map<String, User> userMap = new HashMap<>();

    /**
     * 預設建構子，初始化記憶體使用者資料。
     * <p>
     * 預先插入三筆測試使用者（ID：01、02、03），供 WebService 查詢使用。
     * </p>
     */
    public ExampleWebServiceImpl() {
        logger.info("向實體類插入資料");
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

    /**
     * 依使用者 ID 取得使用者名稱。
     *
     * @param userId 使用者 ID
     * @return 對應的使用者名稱
     */
    @Override
    public String getUserName(String userId) {
        logger.info("userId為：{}", userMap.get(userId).getUserName());
        return userMap.get(userId).getUserName();
    }

    /**
     * 取得所有使用者資料。
     *
     * @return 以使用者 ID 為鍵、{@link User} 物件為值的完整使用者資料表
     */
    @Override
    public Map<String, User> getAllUserData() {
        return userMap;
    }

    /**
     * 依使用者 ID 取得使用者物件。
     *
     * @param userId 使用者 ID
     * @return 對應的 {@link User} 物件；若 ID 不存在則回傳 {@code null}
     */
    @Override
    public User getUser(String userId) {
        logger.info("userMap是:{}", userMap);
        return userMap.get(userId);
    }

}
