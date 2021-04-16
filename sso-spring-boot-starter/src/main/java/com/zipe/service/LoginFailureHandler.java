package com.zipe.service;

import com.zipe.exception.LdapException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

@Slf4j
@Service
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final MessageSource messageSource;

    @Autowired
    public LoginFailureHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
        setDefaultFailureUrl("/login");
        setUseForward(true);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String loginId = request.getParameter("username");
        String address = request.getRemoteAddr();
        log.info("Source ip address : {}", address);
        Locale currentLocale = LocaleContextHolder.getLocale();

        if (exception.getClass().isAssignableFrom(UsernameNotFoundException.class)) {
            // 無此使用者
            logger.warn(messageSource.getMessage("login.user.not.exist", new String[]{loginId}, currentLocale));
        } else if (exception.getClass().isAssignableFrom(DisabledException.class)) {
            // 使用者未啟用
            logger.warn(messageSource.getMessage("login.user.not.enabled", new String[]{loginId}, currentLocale));
        } else if (exception.getClass().isAssignableFrom(BadCredentialsException.class)) {
            // 帳號或密碼錯誤
            logger.warn(messageSource.getMessage("login.access.error.message", new String[]{loginId}, currentLocale));
        } else if (exception.getClass().isAssignableFrom(LdapException.class)) {
            // LDAP 連線愈時
            logger.warn(messageSource.getMessage("login.ldap.connection.timeout.messages", null, currentLocale));
        } else {
            logger.warn(messageSource.getMessage("login.user.verify.fail", null, currentLocale));
        }

        // 使用者登入失敗訊息
        logger.warn(messageSource.getMessage("login.ldap.error.messages", new String[]{loginId, exception.getMessage()}, currentLocale));

        request.setAttribute("error", exception.getMessage());

        super.onAuthenticationFailure(request, response, exception);
    }

}
