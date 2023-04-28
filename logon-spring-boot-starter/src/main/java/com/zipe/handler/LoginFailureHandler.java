package com.zipe.handler;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.exception.LdapException;
import com.zipe.service.CustomLogonLogRecord;
import com.zipe.util.ApplicationContextHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * @author : Gary Tsai
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

    @SneakyThrows
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) {
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

        if(Boolean.TRUE.equals(securityPropertyConfig.getRecordLogEnable())){
            if(StringUtils.isBlank(securityPropertyConfig.getCustomRecordLogBean())){
                throw new Exception("The Custom-Record-Log must have value while Record-Log-Enable = true");
            }
            CustomLogonLogRecord logRecord = (CustomLogonLogRecord) ApplicationContextHelper.getBean(securityPropertyConfig.getCustomRecordLogBean());
            logRecord.recordFailureLog(loginId);
        }

        super.onAuthenticationFailure(request, response, exception);
    }

}
