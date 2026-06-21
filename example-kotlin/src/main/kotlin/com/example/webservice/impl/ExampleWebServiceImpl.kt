package com.example.webservice.impl

import com.example.webservice.ExampleWebService
import com.zipe.model.User
import jakarta.jws.WebService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * [ExampleWebService] 的 CXF WebService 實作類別。
 *
 * 使用記憶體內的 [HashMap] 作為暫存資料儲存，於建構子初始化三筆測試用使用者資料，
 * 供 WebService 端點進行查詢示範。
 *
 * @author Gary.Tsai
 */
@WebService(
    serviceName = "ExampleWebService", // 對外發布的服務名
    targetNamespace = "http://service.example.com", // 指定你想要的命名空間，通常使用套件名稱反轉
    endpointInterface = "com.example.webservice.ExampleWebService" // 服務介面全路徑，指定做 SEI（Service EndPoint Interface）服務端點介面
)
@Component
class ExampleWebServiceImpl : ExampleWebService {

    /** SLF4J 日誌記錄器 */
    private val logger = LoggerFactory.getLogger(ExampleWebServiceImpl::class.java)

    /** 以使用者 ID 為鍵的記憶體使用者資料表，作為本範例的資料來源 */
    private val userMap = HashMap<String, User>()

    /**
     * 預設建構子，初始化記憶體使用者資料。
     *
     * 預先插入三筆測試使用者（ID：01、02、03），供 WebService 查詢使用。
     */
    init {
        logger.info("向實體類插入資料")

        var user = User().apply {
            userId = "01"
            userName = "mracale01"
            email = "mracale01@163.xom"
        }
        userMap[user.userId] = user

        user = User().apply {
            userId = "02"
            userName = "mracale02"
            email = "mracale02@163.xom"
        }
        userMap[user.userId] = user

        user = User().apply {
            userId = "03"
            userName = "mracale03"
            email = "mracale03@163.xom"
        }
        userMap[user.userId] = user
    }

    /**
     * 依使用者 ID 取得使用者名稱。
     *
     * @param userId 使用者 ID
     * @return 對應的使用者名稱
     */
    override fun getUserName(userId: String): String? {
        logger.info("userId為：{}", userMap[userId]?.userName)
        return userMap[userId]?.userName
    }

    /**
     * 取得所有使用者資料。
     *
     * @return 以使用者 ID 為鍵、[User] 物件為值的完整使用者資料表
     */
    override fun getAllUserData(): Map<String, User> = userMap

    /**
     * 依使用者 ID 取得使用者物件。
     *
     * @param userId 使用者 ID
     * @return 對應的 [User] 物件；若 ID 不存在則回傳 `null`
     */
    override fun getUser(userId: String): User? {
        logger.info("userMap是:{}", userMap)
        return userMap[userId]
    }
}
