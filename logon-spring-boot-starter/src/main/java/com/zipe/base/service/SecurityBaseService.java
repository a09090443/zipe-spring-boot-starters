package com.zipe.base.service;

import com.zipe.util.UserInfoUtil;
import com.zipe.vo.SysUserVO;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 09:49
 **/
public class SecurityBaseService {

    @Autowired
    private HttpSession session;

    /**
     * 取得登入者資訊
     *
     * @return
     * @author adam.yeh
     */
    protected SysUserVO fetchLoginUser () {
        String userId = UserInfoUtil.loginUserId();

        if ("anonymousUser".equals(userId)) {
            return null;
        }

        SysUserVO userInfo = null;
        Object userObject = session.getAttribute(userId);

        if (userObject instanceof SysUserVO) {
            userInfo = (SysUserVO) session.getAttribute(userId);
        }

        return userInfo;
    }

}
