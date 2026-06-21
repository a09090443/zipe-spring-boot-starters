package com.zipe.base.service;

import com.zipe.util.UserInfoUtil;
import com.zipe.vo.SysUserVO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 安全性基礎服務，供各業務服務繼承使用。
 * <p>
 * 提供與 Spring Security 整合的共用方法，包含從 Session 取得目前登入使用者資訊等功能。
 * 業務服務可繼承此類別，以重複使用認證相關的基礎邏輯，避免重複程式碼。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 09:49
 **/
public class SecurityBaseService {

    /** 注入 HttpSession，用於存取已登入使用者的 Session 資料 */
    @Autowired
    private HttpSession session;

    /**
     * 取得目前登入使用者的資訊。
     * <p>
     * 透過 {@link UserInfoUtil#loginUserId()} 取得 Spring Security 的使用者識別碼，
     * 再從 Session 中查找對應的 {@link SysUserVO}。
     * 若使用者尚未登入（識別碼為 "anonymousUser"），或 Session 中的物件型別不符，則回傳 {@code null}。
     * </p>
     *
     * @return 已登入使用者的 {@link SysUserVO}；若為匿名使用者或 Session 中無對應資料則回傳 {@code null}
     * @author adam.yeh
     */
    protected SysUserVO fetchLoginUser () {
        String userId = UserInfoUtil.loginUserId();

        // Spring Security 未登入時，Principal 名稱為固定字串 "anonymousUser"，直接回傳 null
        if ("anonymousUser".equals(userId)) {
            return null;
        }

        SysUserVO userInfo = null;
        Object userObject = session.getAttribute(userId);

        // 型別檢查：確保 Session 中儲存的物件確實為 SysUserVO，避免 ClassCastException
        if (userObject instanceof SysUserVO) {
            userInfo = (SysUserVO) session.getAttribute(userId);
        }

        return userInfo;
    }

}
