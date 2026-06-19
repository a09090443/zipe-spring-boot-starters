package com.example.config;

import com.example.model.UserLogin;
import com.example.repository.UserLoginRepository;
import com.zipe.security.GrantedAuthoritiesResolver;
import com.zipe.service.CommonLoginProcess;
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
 *   <li>注入 {@link GrantedAuthoritiesResolver}（由 iam-starter 覆寫為 DB 版本），
 *       在帳密驗證成功後依帳號補上 iam 的群組／權限 authorities——
 *       <b>authn 用自家 {@code user_login}，authz 改由 iam 疊上</b>，
 *       示範認證來源與授權來源解耦。CUSTOM 模式下框架不會自動套用 resolver，
 *       須如本類別於驗證成功時自行呼叫（BASIC／JWT 模式則由 iam 的
 *       {@code IamUserDetailsService} 代為處理）。</li>
 * </ul>
 *
 * <p>啟用方式：將 {@code application.yml} 的 {@code security.verification-type}
 * 由 {@code basic} 改為 {@code custom}，並確認 {@code security.custom-bean-name}
 * 指向本 Bean 名稱 {@code dbAuthProvider} 即可。</p>
 *
 * <p>密碼比對說明：{@code data.sql} 內寫入的是 BCrypt 雜湊字串（無 {@code {id}} 前綴），
 * 因此這裡使用 {@link PasswordEncoder#matches(CharSequence, String)} 將使用者輸入的
 * 原始密碼與資料庫中的雜湊值比對；{@code logon-spring-boot-starter} 預設提供的
 * {@code PasswordEncoder} Bean 為委派式 {@code DelegatingPasswordEncoder}，已設定以
 * {@code BCryptPasswordEncoder} 作為無前綴雜湊的比對後援，故可直接比對既有的 {@code $2a$} 雜湊。</p>
 *
 * @author Gary Tsai
 */
@Slf4j
@Component("dbAuthProvider")
public class DbAuthProvider extends CommonLoginProcess {

    /** 登入帳號資料存取介面，用於依帳號查詢密碼雜湊。 */
    private final UserLoginRepository userLoginRepository;

    /** 授權解析器（iam-starter 覆寫為 DB 版本），依帳號解析 iam 群組／權限。 */
    private final GrantedAuthoritiesResolver authoritiesResolver;

    /**
     * 建構 {@code DbAuthProvider}，注入密碼編碼器、登入帳號 Repository 與授權解析器。
     *
     * @param passwordEncoder     Spring Security 密碼編碼器（委派式，相容無前綴 BCrypt），傳遞給父類別使用
     * @param userLoginRepository 登入帳號資料存取介面
     * @param authoritiesResolver iam 授權解析器，用於登入成功後補上群組／權限
     */
    public DbAuthProvider(PasswordEncoder passwordEncoder, UserLoginRepository userLoginRepository,
                          GrantedAuthoritiesResolver authoritiesResolver) {
        super(passwordEncoder);
        this.userLoginRepository = userLoginRepository;
        this.authoritiesResolver = authoritiesResolver;
    }

    /**
     * 以資料庫帳號驗證一般使用者的帳號與密碼。
     *
     * <p>流程說明：</p>
     * <ol>
     *   <li>依 {@code loginId} 查詢 {@code user_login} 資料表。</li>
     *   <li>查無帳號時拋出 {@link BadCredentialsException}（不揭露是帳號或密碼錯誤）。</li>
     *   <li>以 {@link PasswordEncoder#matches} 比對輸入密碼與儲存的 BCrypt 雜湊。</li>
     *   <li>以 {@link GrantedAuthoritiesResolver} 依帳號解析 iam 群組／權限，
     *       作為已認證 token 的 authorities（查無對應時為空集合）。</li>
     *   <li>驗證成功回傳已認證的 {@link UsernamePasswordAuthenticationToken}，
     *       credentials 設為 null 以避免明文密碼殘留於安全內容中。</li>
     * </ol>
     *
     * @param loginId  登入帳號
     * @param password 使用者輸入的原始密碼（明文）
     * @return 已認證的 {@link UsernamePasswordAuthenticationToken}，authorities 來自 iam
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
        // 帳密由 user_login 驗證（authn），群組／權限改由 iam 解析（authz）——兩者解耦
        var authorities = authoritiesResolver.resolve(loginId);
        log.info("使用者:{} 登入成功，iam 授權:{}", loginId, authorities);
        // 帶入 iam authorities 使 token 成為已認證狀態，並清除明文密碼
        return new UsernamePasswordAuthenticationToken(loginId, null, authorities);
    }
}
