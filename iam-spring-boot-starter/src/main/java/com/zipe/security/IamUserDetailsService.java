package com.zipe.security;

import com.zipe.entity.Account;
import com.zipe.repository.AccountRepository;
import com.zipe.service.BasicUserServiceImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 以 iam 帳號表為來源的 {@link UserDetails} 服務，作為 BASIC 模式的認證接點。
 * <p>
 * 繼承 logon-starter 的 {@link BasicUserServiceImpl}，覆寫 {@code loadUserByUsername}：
 * 由 iam 帳號表提供 username／password／enabled／locked，authorities 則委由
 * {@link GrantedAuthoritiesResolver} 解析（與 LDAP 模式共用同一套授權來源）。
 * </p>
 * <p>
 * 此 Bean 由 {@code IamAutoConfiguration} 以 {@link BasicUserServiceImpl} 型別、
 * 搭配 {@code @ConditionalOnMissingBean} 註冊，使 logon 寫死 {@code admin} 的預設
 * {@code basicUserServiceImpl} 自動退讓，登入帳號改由資料庫帳號表提供。
 * </p>
 *
 * @author Gary.Tsai
 */
public class IamUserDetailsService extends BasicUserServiceImpl {

    /** 帳號資料存取層，供以登入名稱查詢帳號的密碼與狀態。 */
    private final AccountRepository accountRepository;

    /** 授權解析器，依帳號名稱解析其 authorities。 */
    private final GrantedAuthoritiesResolver authoritiesResolver;

    /**
     * 建構 iam 使用者服務，注入密碼編碼器、帳號 Repository 與授權解析器。
     * <p>
     * 須呼叫 {@code super(passwordEncoder)} 沿用父類別的密碼編碼器欄位；本類別載入的帳號
     * 密碼已是 BCrypt 雜湊，故 {@code loadUserByUsername} 不再以 encoder 重複編碼。
     * </p>
     *
     * @param passwordEncoder     密碼編碼器（由 logon 容器提供）
     * @param accountRepository   帳號 Repository
     * @param authoritiesResolver 授權解析器
     */
    public IamUserDetailsService(PasswordEncoder passwordEncoder,
                                 AccountRepository accountRepository,
                                 GrantedAuthoritiesResolver authoritiesResolver) {
        super(passwordEncoder);
        this.accountRepository = accountRepository;
        this.authoritiesResolver = authoritiesResolver;
    }

    /**
     * 依登入名稱由 iam 帳號表載入使用者詳細資訊。
     * <p>
     * 密碼欄位為已雜湊的 BCrypt 字串，直接交給 {@link User.UserBuilder#password} 而不再編碼；
     * 帳號的 {@code enabled}／{@code locked} 對應 Spring Security 的啟用與帳號鎖定狀態；
     * authorities 由 {@link GrantedAuthoritiesResolver#resolve(String)} 提供。
     * </p>
     *
     * @param username 使用者輸入的登入名稱
     * @return 包含帳號、密碼、狀態與權限的 {@link UserDetails}
     * @throws UsernameNotFoundException 當帳號不存在時拋出
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.builder()
                .username(account.getUsername())
                .password(account.getPassword())
                .disabled(!Boolean.TRUE.equals(account.getEnabled()))
                .accountLocked(Boolean.TRUE.equals(account.getLocked()))
                .authorities(authoritiesResolver.resolve(username))
                .build();
    }
}
