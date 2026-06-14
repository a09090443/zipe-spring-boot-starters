package com.example.config;

import com.example.model.UserLogin;
import com.example.repository.UserLoginRepository;
import com.zipe.service.CommonLoginProcess;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 業務端自訂登入（CUSTOM 模式）的 {@link com.zipe.service.CommonLoginProcess} 實作範例。
 *
 * <p>本類別示範如何在業務系統中接上自家的使用者資料庫進行帳號密碼驗證：</p>
 * <ul>
 *   <li>繼承 {@link CommonLoginProcess}，因此自動沿用父類別的 ADMIN 動態密碼機制
 *       （以當日日期作為密碼，無須覆寫即可使用）。</li>
 *   <li>覆寫 {@link #verifyNormalUser(String, String)}，以 {@link UserLoginRepository}
 *       查詢 {@code user_login} 資料表中的帳號，並用注入的 {@link PasswordEncoder}
 *       比對密碼。</li>
 * </ul>
 *
 * <p>啟用方式：將 {@code application.yml} 的 {@code security.verification-type}
 * 由 {@code basic} 改為 {@code custom}，並確認 {@code security.custom-bean-name}
 * 指向本 Bean 名稱 {@code dbAuthProvider} 即可。</p>
 *
 * <p>密碼比對說明：{@code data.sql} 內寫入的是 BCrypt 雜湊字串，
 * 因此這裡使用 {@link PasswordEncoder#matches(CharSequence, String)} 將使用者輸入的
 * 原始密碼與資料庫中的雜湊值比對；{@code logon-spring-boot-starter} 預設提供的
 * {@code PasswordEncoder} Bean 即為 {@code BCryptPasswordEncoder}。</p>
 *
 * @author Gary Tsai
 */
@Slf4j
@Component("dbAuthProvider")
public class DbAuthProvider extends CommonLoginProcess {

    /** 登入帳號資料存取介面，用於依帳號查詢密碼雜湊。 */
    private final UserLoginRepository userLoginRepository;

    /**
     * 建構 {@code DbAuthProvider}，注入密碼編碼器與登入帳號 Repository。
     *
     * @param passwordEncoder     Spring Security 密碼編碼器（BCrypt），傳遞給父類別使用
     * @param userLoginRepository 登入帳號資料存取介面
     */
    public DbAuthProvider(PasswordEncoder passwordEncoder, UserLoginRepository userLoginRepository) {
        super(passwordEncoder);
        this.userLoginRepository = userLoginRepository;
    }

    /**
     * 以資料庫帳號驗證一般使用者的帳號與密碼。
     *
     * <p>流程說明：</p>
     * <ol>
     *   <li>依 {@code loginId} 查詢 {@code user_login} 資料表。</li>
     *   <li>查無帳號時拋出 {@link BadCredentialsException}（不揭露是帳號或密碼錯誤）。</li>
     *   <li>以 {@link PasswordEncoder#matches} 比對輸入密碼與儲存的 BCrypt 雜湊。</li>
     *   <li>驗證成功回傳已認證的 {@link UsernamePasswordAuthenticationToken}，
     *       credentials 設為 null 以避免明文密碼殘留於安全內容中。</li>
     * </ol>
     *
     * @param loginId  登入帳號
     * @param password 使用者輸入的原始密碼（明文）
     * @return 已認證的 {@link UsernamePasswordAuthenticationToken}
     * @throws BadCredentialsException 帳號不存在或密碼比對失敗時拋出
     */
    @Override
    protected UsernamePasswordAuthenticationToken verifyNormalUser(String loginId, String password) {
        UserLogin userLogin = userLoginRepository.findByLoginId(loginId);
        if (userLogin == null) {
            log.warn("使用者:{} 帳號不存在", loginId);
            throw new BadCredentialsException("使用者:" + loginId + " 帳號或密碼錯誤");
        }
        if (!passwordEncoder.matches(password, userLogin.getPassword())) {
            log.warn("使用者:{} 密碼錯誤", loginId);
            throw new BadCredentialsException("使用者:" + loginId + " 帳號或密碼錯誤");
        }
        log.info("使用者:{} 登入成功", loginId);
        // 使用非 null 的權限集合使 token 成為已認證狀態，並清除明文密碼
        return new UsernamePasswordAuthenticationToken(loginId, null, Collections.emptyList());
    }
}
