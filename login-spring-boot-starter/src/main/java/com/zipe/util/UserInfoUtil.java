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
     * 取得登入者ID
     *
     * @return
     */
    public static String loginUserId() {
        String userId;
        Object principal = "";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            principal = auth.getPrincipal();
        }

        if (principal instanceof UserDetails) {
            userId = ((UserDetails) principal).getUsername();
        } else {
            userId = principal.toString();
        }

        return userId;
    }
}
