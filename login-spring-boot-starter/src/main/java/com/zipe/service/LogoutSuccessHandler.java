package com.zipe.service;

import com.zipe.util.UserInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Slf4j
@Service
public class LogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final HttpSession session;

    @Autowired
    public LogoutSuccessHandler(HttpSession session) {
        setDefaultTargetUrl("/login");
        setAlwaysUseDefaultTargetUrl(true);
        this.session = session;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        destroyLoginUserInfo();

        if (authentication != null) {
            log.debug("user:" + authentication.getPrincipal().toString() + "logout" + request.getContextPath());
        }

        super.onLogoutSuccess(request, response, authentication);
    }

    private void destroyLoginUserInfo () {
        String userId = UserInfoUtil.loginUserId();
        session.removeAttribute(userId);
    }

}
