package com.zipe.service;

import com.zipe.enums.UserEnum;
import com.zipe.util.string.StringConstant;
import com.zipe.util.time.DateTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author gary.tsai 2019/8/26
 */
public abstract class CommonLoginProcess implements AuthenticationProvider {

    protected final PasswordEncoder passwordEncoder;

    @Autowired
    protected CommonLoginProcess(PasswordEncoder passwordEncoder){
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String loginId = authentication.getName();
        String password = (String) authentication.getCredentials();

        if (StringUtils.isBlank(loginId)) {
            throw new UsernameNotFoundException(StringConstant.BLANK);
        }

        if (UserEnum.ADMIN.name().equalsIgnoreCase(authentication.getName())) {
            return this.verifySpecialUser(loginId, password);
        } else {
            return verifyNormalUser(loginId, password);
        }
    }

    /**
     * 特殊使用者認證程序，如:系統管理者
     */
    public UsernamePasswordAuthenticationToken verifySpecialUser(String userName, String password) {
        // 以當前日期為密碼
        String dynamicPassword = DateTimeUtils.getDateNow(DateTimeUtils.dateTimeFormate7);
        if (!dynamicPassword.equalsIgnoreCase(password)) {
            throw new BadCredentialsException(StringConstant.BLANK);
        }
        return new UsernamePasswordAuthenticationToken(userName, password, null);
    }

    /**
     * 使用者認證程序
     */
    protected abstract UsernamePasswordAuthenticationToken verifyNormalUser(String loginId, String password);

}
