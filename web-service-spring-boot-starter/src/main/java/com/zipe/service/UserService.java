package com.zipe.service;

import com.zipe.model.User;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import java.util.Map;

/**
 * 使用者 WebService 介面（範例）。
 * <p>
 * 宣告對外公開的 SOAP WebService 操作，由 Apache CXF 框架自動發布。
 * {@code targetNamespace} 須明確指定；若省略，動態呼叫（invoke）時
 * 將因找不到介面方法而失敗，具體原因與 CXF 的 WSDL 命名空間解析機制有關。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/5/4 下午 02:05
 **/
// 若不指定 targetNamespace，動態呼叫 invoke 時會找不到介面方法，具體原因與 CXF WSDL 解析有關。
@WebService(targetNamespace = "http://service.zipe.com")
public interface UserService {

    /**
     * 依使用者 ID 查詢單一使用者資訊。
     * <p>
     * {@code @WebMethod(action = "getUser")} 將此方法標記為 SOAP Action，
     * 用於向外部客戶端公開；即使省略此標記功能仍可運作，但明確宣告有助於
     * WSDL 文件的可讀性與客戶端路由。
     * </p>
     *
     * @param userId 使用者唯一識別碼
     * @return 對應的 {@link User} 物件；若不存在則依實作決定回傳 {@code null} 或拋出例外
     */
    @WebMethod(action = "getUser") // action 對應 SOAP Action，有助於客戶端路由與 WSDL 可讀性
    public User getUser(@WebParam(name = "userId") String userId);

    /**
     * 依使用者 ID 查詢使用者名稱。
     *
     * @param userId 使用者唯一識別碼
     * @return 使用者名稱字串；若不存在則依實作決定回傳 {@code null} 或空字串
     */
    @WebMethod
    @WebResult(name = "String", targetNamespace = "")
    public String getUserName(@WebParam(name = "userId") String userId);

    /**
     * 取得所有使用者資料的映射表。
     *
     * @return 以使用者 ID 為鍵、{@link User} 物件為值的 {@link Map}
     */
    @WebMethod
    @WebResult(name = "Map")
    public Map<String, User> getAllUserData();
}
