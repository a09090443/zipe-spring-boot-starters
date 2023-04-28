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
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/20 上午 11:43
 **/
@Slf4j
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final SecurityPropertyConfig securityPropertyConfig;

    public LoginSuccessHandler(SecurityPropertyConfig securityPropertyConfig) {
        setDefaultTargetUrl(securityPropertyConfig.getLoginSuccessUri());
        setAlwaysUseDefaultTargetUrl(false);
        this.securityPropertyConfig = securityPropertyConfig;
    }

    @SneakyThrows
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        log.debug("User:{}, login uri:{}", UserInfoUtil.loginUserId(), request.getContextPath());
        log.debug("Client ip:{}", getIpAddress(request));
        if(securityPropertyConfig.getRecordLogEnable()){
            if(StringUtils.isBlank(securityPropertyConfig.getCustomRecordLogBean())){
                throw new Exception("The Custom-Record-Log must have value while Record-Log-Enable = true");
            }
            CustomLogonLogRecord logRecord = (CustomLogonLogRecord) ApplicationContextHelper.getBean(securityPropertyConfig.getCustomRecordLogBean());
            logRecord.recordLoginSuccessLog(UserInfoUtil.loginUserId());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
