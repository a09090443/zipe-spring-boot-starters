package com.zipe.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 取得登入者資訊
 *
 * @author gary.tsai, adam.yeh 2019/7/5
 */
public class UserInfoUtil {
    /**
     * 取得目前已登入使用者的帳號（Username）。
     *
     * <p>從 Spring Security 的 {@link SecurityContextHolder} 中讀取認證資訊，
     * 若 principal 為 {@link UserDetails} 實例則取 username，
     * 否則直接對 principal 呼叫 {@code toString()}（例如匿名使用者字串 "anonymousUser"）。
     *
     * @return 登入者帳號字串；若尚未認證則回傳 principal 的字串表示（通常為 "anonymousUser"）
     */
    public static String loginUserId() {
        String userId;
        // 預設為空字串，避免 Authentication 為 null 時 principal 尚未賦值
        Object principal = "";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            principal = auth.getPrincipal();
        }

        // UserDetails 代表已正常登入的使用者，優先透過 getUsername() 取得帳號
        if (principal instanceof UserDetails) {
            userId = ((UserDetails) principal).getUsername();
        } else {
            // 非 UserDetails 時（如匿名使用者），直接轉字串
            userId = principal.toString();
        }

        return userId;
    }
}
