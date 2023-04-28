package com.zipe.handler;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.service.CustomLogonLogRecord;
import com.zipe.util.ApplicationContextHelper;
import com.zipe.util.UserInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * @author : Gary Tsai
 **/
@Slf4j
public class LogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final SecurityPropertyConfig securityPropertyConfig;

    @Autowired
    public LogoutSuccessHandler(SecurityPropertyConfig securityPropertyConfig) {
        this.securityPropertyConfig = securityPropertyConfig;
        setDefaultTargetUrl(securityPropertyConfig.getLoginSuccessUri());
        setAlwaysUseDefaultTargetUrl(true);
    }

    @SneakyThrows
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        destroyLoginUserInfo(request);

        if (authentication != null) {
            log.debug("user:{}, logout:{}", authentication.getPrincipal().toString(), request.getContextPath());
        }


        if(Boolean.TRUE.equals(securityPropertyConfig.getRecordLogEnable())){
            if(StringUtils.isBlank(securityPropertyConfig.getCustomRecordLogBean())){
                throw new Exception("The Custom-Record-Log must have value while Record-Log-Enable = true");
            }
            CustomLogonLogRecord logRecord = (CustomLogonLogRecord) ApplicationContextHelper.getBean(securityPropertyConfig.getCustomRecordLogBean());
            logRecord.recordFailureLog(UserInfoUtil.loginUserId());
        }

        super.onLogoutSuccess(request, response, authentication);
    }

    private void destroyLoginUserInfo(HttpServletRequest request) {
        String userId = UserInfoUtil.loginUserId();
        request.getSession().removeAttribute(userId);
    }

}
