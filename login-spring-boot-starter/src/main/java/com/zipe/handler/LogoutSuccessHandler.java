package com.zipe.handler;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.util.UserInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/20 下午 13:46
 **/
@Slf4j
public class LogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final SecurityPropertyConfig securityPropertyConfig;

    @Autowired
    public LogoutSuccessHandler(SecurityPropertyConfig securityPropertyConfig) {
        this.securityPropertyConfig = securityPropertyConfig;
        setDefaultTargetUrl(securityPropertyConfig.getLoginUri());
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        destroyLoginUserInfo(request);

        if (authentication != null) {
            log.debug("user:{}, logout:{}", authentication.getPrincipal().toString(), request.getContextPath());
        }

        super.onLogoutSuccess(request, response, authentication);
    }

    private void destroyLoginUserInfo(HttpServletRequest request) {
        String userId = UserInfoUtil.loginUserId();
        request.getSession().removeAttribute(userId);
    }

}
