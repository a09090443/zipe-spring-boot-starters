package com.zipe.handler;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.exception.LdapException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/20 上午 11:52
 **/
@Slf4j
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final SecurityPropertyConfig securityPropertyConfig;

    @Autowired
    public LoginFailureHandler(SecurityPropertyConfig securityPropertyConfig) {
        this.securityPropertyConfig = securityPropertyConfig;
        setDefaultFailureUrl(securityPropertyConfig.getLoginFailureUri());
        setUseForward(true);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String loginId = request.getParameter("username");
        String address = request.getRemoteAddr();
        log.info("Client ip address : {}", address);

        if (exception.getClass().isAssignableFrom(UsernameNotFoundException.class)) {
            // 無此使用者
            log.warn("使用者:{} 不存在", loginId);
        } else if (exception.getClass().isAssignableFrom(DisabledException.class)) {
            // 使用者未啟用
            log.warn("使用者:{} 未啟用", loginId);
        } else if (exception.getClass().isAssignableFrom(BadCredentialsException.class)) {
            // 帳號或密碼錯誤
            log.warn("使用者:{} 帳號或密碼錯誤", loginId);
        } else if (exception.getClass().isAssignableFrom(LdapException.class)) {
            // LDAP 連線愈時
            log.warn("帳號系統連線無回應");
        } else {
            log.warn("登入錯誤");
        }

        super.onAuthenticationFailure(request, response, exception);
    }

}
