package com.example.webservice

import com.zipe.model.User
import jakarta.jws.WebMethod
import jakarta.jws.WebParam
import jakarta.jws.WebResult
import jakarta.jws.WebService

/**
 * 使用者 WebService 介面，透過 CXF 以 SOAP 協定對外公開使用者查詢功能。
 *
 * 定義取得單一使用者、使用者名稱以及全部使用者資料的操作，
 * 由 [com.example.webservice.impl.ExampleWebServiceImpl] 提供具體實作。
 *
 * @author Gary Tsai
 */
// 若未指定 targetNamespace，動態呼叫 invoke 時會找不到介面內的方法，原因待查；加上後可正常運作。
@WebService(name = "UserService", targetNamespace = "http://service.example.com")
interface ExampleWebService {

    /**
     * 根據使用者 ID 查詢對應的使用者資料。
     *
     * @param userId 使用者識別碼
     * @return 對應的 [User] 物件；若查無資料則回傳 `null`
     */
    @WebMethod // 標示此方法為對外公開的 WebService 操作；即使省略，行為不受影響，但建議保留以增加可讀性。
    fun getUser(@WebParam(name = "userId", targetNamespace = "http://service.example.com") userId: String): User?

    /**
     * 根據使用者 ID 查詢對應的使用者名稱。
     *
     * @param userId 使用者識別碼
     * @return 使用者名稱字串；若查無資料則回傳 `null`
     */
    @WebMethod
    fun getUserName(@WebParam(name = "userId", targetNamespace = "http://service.example.com") userId: String): String?

    /**
     * 取得所有使用者資料，以使用者 ID 為鍵、[User] 物件為值的 Map 形式回傳。
     *
     * @return 包含全部使用者資料的 [Map]，鍵為使用者 ID
     */
    @WebMethod
    @WebResult(name = "Map")
    fun getAllUserData(): Map<String, User>
}
