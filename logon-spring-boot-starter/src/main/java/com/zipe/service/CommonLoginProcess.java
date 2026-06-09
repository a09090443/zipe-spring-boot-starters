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
 * 共用登入處理流程的抽象基底類別。
 * <p>
 * 實作 Spring Security 的 {@link AuthenticationProvider}，提供帳號密碼驗證的骨架流程：
 * <ul>
 *   <li>若登入帳號為 ADMIN，委派至 {@link #verifySpecialUser} 以動態密碼（當日日期）驗證。</li>
 *   <li>其餘帳號委派至子類別實作的 {@link #verifyNormalUser} 進行驗證（支援 DB 或 LDAP 等方式）。</li>
 * </ul>
 * 子類別需實作 {@link #verifyNormalUser} 以完成對應的驗證邏輯。
 *
 * @author gary.tsai 2019/8/26
 */
public abstract class CommonLoginProcess implements AuthenticationProvider {

    /** 密碼編碼器，用於比對儲存的雜湊密碼與輸入的原始密碼 */
    protected final PasswordEncoder passwordEncoder;

    /**
     * 以依賴注入方式取得 {@link PasswordEncoder}。
     *
     * @param passwordEncoder Spring Security 密碼編碼器
     */
    @Autowired
    protected CommonLoginProcess(PasswordEncoder passwordEncoder){
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 宣告本 Provider 僅支援 {@link UsernamePasswordAuthenticationToken} 類型的認證請求。
     *
     * @param authentication 欲驗證的 Authentication 類別
     * @return 若為 {@code UsernamePasswordAuthenticationToken} 則回傳 {@code true}，否則 {@code false}
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    /**
     * 執行帳號密碼認證的主流程。
     * <p>
     * 依帳號名稱分流：ADMIN 帳號走動態密碼驗證，其餘帳號交由子類別的一般驗證流程處理。
     *
     * @param authentication 包含帳號與密碼的認證請求物件
     * @return 驗證成功後的 {@link Authentication} 物件
     * @throws AuthenticationException 帳號空白或驗證失敗時拋出
     */
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
     * <p>
     * 以當日日期字串（格式由 {@link DateTimeUtils#dateTimeFormate7} 定義）作為動態密碼；
     * 此設計無需在資料庫儲存 ADMIN 密碼，同時讓密碼每日自動更換以提高安全性。
     *
     * @param userName 特殊使用者帳號名稱
     * @param password 使用者輸入的密碼
     * @return 驗證成功的 {@link UsernamePasswordAuthenticationToken}（授權集合為 null）
     * @throws BadCredentialsException 輸入密碼與當日動態密碼不符時拋出
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
     * <p>
     * 子類別須覆寫此方法，依實際驗證來源（資料庫、LDAP 等）完成密碼比對並回傳認證 Token。
     *
     * @param loginId 登入帳號
     * @param password 使用者輸入的原始密碼
     * @return 驗證成功的 {@link UsernamePasswordAuthenticationToken}
     */
    protected abstract UsernamePasswordAuthenticationToken verifyNormalUser(String loginId, String password);

}
