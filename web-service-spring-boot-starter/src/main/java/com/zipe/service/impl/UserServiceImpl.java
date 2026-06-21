package com.zipe.service.impl;

import com.zipe.model.User;
import com.zipe.service.UserService;
import jakarta.jws.WebService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Component;

/**
 * {@link UserService} 的 SOAP WebService 實作類別，作為示範用途。
 * <p>
 * 透過 Apache CXF 的 {@code @WebService} 對外發布 SOAP 端點，
 * 並於建構時預先填入三筆測試使用者資料供驗證使用。
 * </p>
 *
 * @ClassName:UserServiceImpl
 * @Description:測試服務介面實作類別
 */
@Slf4j
@WebService(serviceName = "UserService",//對外發布的服務名
        targetNamespace = "http://service.zipe.com",//指定你想要的名稱空間，通常使用使用包名反轉
        endpointInterface = "com.zipe.service.UserService")//服務介面全路徑, 指定做SEI（Service EndPoint Interface）服務端點介面
@Component
public class UserServiceImpl implements UserService {

    /** 以使用者名稱（userName）為鍵，存放所有測試使用者資料的記憶體快取。 */
    private Map<String, User> userMap = new HashMap<String, User>();

    /**
     * 預設建構子，初始化三筆測試使用者資料並存入 {@link #userMap}。
     * <p>
     * 第三筆使用者的 email 欄位刻意包裝成 HTML 標籤字串，
     * 並透過 {@code StringEscapeUtils.unescapeHtml4} 還原為純文字，
     * 以驗證 HTML 跳脫字元的處理能力。
     * </p>
     */
    public UserServiceImpl() {
        log.info("向實體類插入資料");
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
        // 模擬含 HTML 標籤的 email 字串，驗證 unescapeHtml4 能正確去除外層標籤
        String email = "<email>mracale03@163.xom</email>";
        String unescaped = StringEscapeUtils.unescapeHtml4(email);
        user.setEmail(unescaped);
        userMap.put(user.getUserName(), user);
    }

    /**
     * 依據使用者 ID 回傳格式化的使用者 ID 字串。
     *
     * @param userId 使用者 ID
     * @return 包含傳入 userId 的說明字串
     */
    @Override
    public String getUserName(String userId) {
        return "userId為：" + userId;
    }

    /**
     * 取得所有測試使用者資料的對應表。
     *
     * @return 以使用者名稱（userName）為鍵的 {@link User} 對應表
     */
    @Override
    public Map<String, User> getAllUserData() {
        return userMap;
    }

    /**
     * 依據使用者 ID（對應 userName）從記憶體中查詢使用者資料。
     *
     * @param userId 使用者 ID（此實作以 userName 為查詢鍵）
     * @return 對應的 {@link User} 物件；若不存在則回傳 {@code null}
     */
    @Override
    public User getUser(String userId) {
        log.info("userMap是:{}", userMap);
        return userMap.get(userId);
    }

}
