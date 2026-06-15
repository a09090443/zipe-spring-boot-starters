package com.zipe.autoconfiguration;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.enums.VerificationTypeEnum;
import com.zipe.handler.LoginFailureHandler;
import com.zipe.handler.LoginSuccessHandler;
import com.zipe.handler.LogoutSuccessHandler;
import com.zipe.jwt.JwtAuthenticationFilter;
import com.zipe.jwt.JwtLoginController;
import com.zipe.jwt.JwtProperties;
import com.zipe.jwt.JwtTokenProvider;
import com.zipe.service.BasicUserServiceImpl;
import com.zipe.service.LdapUserDetailsService;
import com.zipe.util.ApplicationContextHelper;
import com.zipe.util.string.StringConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security 主要自動配置類別。
 *
 * <p>依據 {@link SecurityPropertyConfig} 的設定，動態選擇登入流程（預設表單登入或自訂登入頁面）、
 * 認證提供者（BASIC / LDAP / CUSTOM）、CSRF 防護及 X-Frame-Options 政策。
 * 同時宣告 {@link PasswordEncoder}、{@link SessionRegistry} 與各登入事件處理器等共用 Bean。</p>
 */
@Slf4j
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({SecurityPropertyConfig.class, JwtProperties.class})
public class SecurityConfiguration {

    /** 開放所有路徑的萬用 URI 樣式，僅在安全性停用時使用。 */
    private final String PERMIT_ALL = "/**";

    /** Security 相關設定屬性，由 {@code application.yml} 注入。 */
    private final SecurityPropertyConfig securityPropertyConfig;

    /**
     * 建構子，透過建構子注入 Security 設定屬性。
     *
     * @param securityPropertyConfig Security 設定屬性物件
     */
    public SecurityConfiguration(SecurityPropertyConfig securityPropertyConfig) {
        this.securityPropertyConfig = securityPropertyConfig;
    }

    // ============================ JWT 無狀態模式 ============================
    // 僅當 security.jwt.enabled=true 時生效，與 security.verification-type（憑證來源）正交：
    // 登入仍依 verification-type（BASIC / LDAP / CUSTOM）驗帳密，驗證成功後改發 JWT。
    // 本 filter chain 先於預設 filterChain 註冊，使其 @ConditionalOnMissingBean(SecurityFilterChain.class)
    // 自動退讓。各 Bean 皆標註 @ConditionalOnMissingBean，業務專案可自行覆寫。

    /**
     * 建立 JWT 無狀態模式的 {@link SecurityFilterChain}。
     *
     * <p>停用 CSRF 與 Session（{@link SessionCreationPolicy#STATELESS}），放行登入端點與
     * {@code security.allow-uris}，其餘請求須通過 {@link JwtAuthenticationFilter} 驗證。</p>
     *
     * @param http                   {@link HttpSecurity} 設定建構器
     * @param jwtAuthenticationFilter JWT 驗證過濾器（容器 Bean）
     * @param jwtProperties          JWT 設定屬性（容器 Bean）
     * @return 建置完成的 {@link SecurityFilterChain}
     * @throws Exception 設定過程發生例外時拋出
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http,
                                                      JwtAuthenticationFilter jwtAuthenticationFilter,
                                                      JwtProperties jwtProperties) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(switchSecurity()).permitAll()
                        .requestMatchers(jwtProperties.getLoginUri()).permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        configureFrameOptions(http);
        return http.build();
    }

    /**
     * 建立 {@link JwtTokenProvider}，由 Spring 觸發其 {@code @PostConstruct init()}。
     *
     * @param jwtProperties JWT 設定屬性（容器 Bean）
     * @return {@link JwtTokenProvider} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
    public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(jwtProperties);
    }

    /**
     * 建立 {@link JwtAuthenticationFilter}，負責每次請求驗 token 並查權限。
     *
     * <p>查權限以具體型別注入 {@link com.zipe.service.BasicUserServiceImpl}（避免容器多個
     * {@code UserDetailsService} Bean 歧義）。由於 LDAP / CUSTOM 的驗證需要密碼、無法僅以
     * username 重新查詢，{@code verification-type=ldap|custom} 搭配 JWT 時，請覆寫
     * {@code basicUserServiceImpl} Bean，提供可依 username 載入權限的 {@code UserDetailsService}
     * 實作，否則一般使用者請求會因查無使用者而被拒。</p>
     *
     * @param provider         JWT 簽驗核心（容器 Bean）
     * @param basicUserService 查權限用的使用者服務（容器 Bean，可覆寫）
     * @param jwtProperties    JWT 設定屬性（容器 Bean）
     * @return {@link JwtAuthenticationFilter} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider,
                                                           BasicUserServiceImpl basicUserService,
                                                           JwtProperties jwtProperties) {
        return new JwtAuthenticationFilter(provider, basicUserService, jwtProperties);
    }

    /**
     * 建立 JWT 模式登入用的 {@link AuthenticationManager}。
     *
     * <p>重用與表單登入相同的 provider 選擇邏輯（{@link #resolveAuthenticationProvider}），
     * 因此登入會依 {@code security.verification-type} 走 BASIC / LDAP / CUSTOM 驗帳密，
     * 而非寫死 BASIC。業務專案可覆寫本 Bean 完全接管登入驗證。</p>
     *
     * @param basicUserService BASIC 模式使用者服務（容器 Bean）
     * @param ldapUserService  LDAP 模式使用者服務（容器 Bean）
     * @param passwordEncoder  密碼編碼器（容器 Bean）
     * @return {@link AuthenticationManager} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
    public AuthenticationManager jwtAuthenticationManager(BasicUserServiceImpl basicUserService,
                                                          LdapUserDetailsService ldapUserService,
                                                          PasswordEncoder passwordEncoder) {
        return new ProviderManager(
                resolveAuthenticationProvider(basicUserService, ldapUserService, passwordEncoder));
    }

    /**
     * 建立內建 JWT 登入端點 {@link JwtLoginController}。
     *
     * @param authenticationManager JWT 模式的認證管理器（容器 Bean）
     * @param provider              JWT 簽驗核心（容器 Bean）
     * @return {@link JwtLoginController} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
    public JwtLoginController jwtLoginController(AuthenticationManager authenticationManager,
                                                JwtTokenProvider provider) {
        return new JwtLoginController(authenticationManager, provider);
    }

    // ============================ 預設（BASIC / LDAP / CUSTOM）模式 ============================

    /**
     * 建立並回傳主要的 {@link SecurityFilterChain}。
     *
     * <p>若設定了自訂登入頁 URI（{@code security.login-uri}），則套用自訂登入流程；
     * 否則使用 Spring Security 預設表單登入流程。</p>
     *
     * @param http                {@link HttpSecurity} 設定建構器
     * @param basicUserService    BASIC 模式使用的 {@link BasicUserServiceImpl}（容器 Bean，含使用者覆寫版）
     * @param ldapUserService     LDAP 模式使用的 {@link LdapUserDetailsService}（容器 Bean）
     * @param passwordEncoder     密碼編碼器（容器 Bean，含使用者覆寫版）
     * @param sessionRegistry     Session 登錄表（容器 Bean）
     * @param loginSuccessHandler 登入成功處理器（容器 Bean）
     * @param loginFailureHandler 登入失敗處理器（容器 Bean）
     * @param logoutSuccessHandler 登出成功處理器（容器 Bean）
     * @return 建置完成的 {@link SecurityFilterChain}
     * @throws Exception 設定過程發生例外時拋出
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           BasicUserServiceImpl basicUserService,
                                           LdapUserDetailsService ldapUserService,
                                           PasswordEncoder passwordEncoder,
                                           SessionRegistry sessionRegistry,
                                           LoginSuccessHandler loginSuccessHandler,
                                           LoginFailureHandler loginFailureHandler,
                                           LogoutSuccessHandler logoutSuccessHandler) throws Exception {
        if (StringUtils.isNotBlank(securityPropertyConfig.getLoginUri())) {
            customLoginConfigure(http, sessionRegistry, loginSuccessHandler, loginFailureHandler,
                    logoutSuccessHandler, basicUserService, ldapUserService, passwordEncoder);
        } else {
            basicLoginConfigure(http, sessionRegistry, loginSuccessHandler, loginFailureHandler,
                    logoutSuccessHandler, basicUserService, ldapUserService, passwordEncoder);
        }
        return http.build();
    }

    /**
     * 套用 Spring Security 預設表單登入的安全設定。
     *
     * <p>使用內建登入頁面，並設定登入成功/失敗處理器、登出行為與 Session 管理。</p>
     *
     * @param http                 {@link HttpSecurity} 設定建構器
     * @param sessionRegistry      Session 登錄表（容器 Bean）
     * @param loginSuccessHandler  登入成功處理器（容器 Bean）
     * @param loginFailureHandler  登入失敗處理器（容器 Bean）
     * @param logoutSuccessHandler 登出成功處理器（容器 Bean）
     * @param basicUserService     BASIC 模式 {@link BasicUserServiceImpl}（容器 Bean）
     * @param ldapUserService      LDAP 模式 {@link LdapUserDetailsService}（容器 Bean）
     * @param passwordEncoder      密碼編碼器（容器 Bean）
     * @throws Exception 設定過程發生例外時拋出
     */
    private void basicLoginConfigure(HttpSecurity http,
                                     SessionRegistry sessionRegistry,
                                     LoginSuccessHandler loginSuccessHandler,
                                     LoginFailureHandler loginFailureHandler,
                                     LogoutSuccessHandler logoutSuccessHandler,
                                     BasicUserServiceImpl basicUserService,
                                     LdapUserDetailsService ldapUserService,
                                     PasswordEncoder passwordEncoder) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(switchSecurity()).permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(formLogin -> formLogin.permitAll()
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler))
                .logout(logout -> logout.logoutUrl("/login")
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .permitAll()
                        .logoutSuccessHandler(logoutSuccessHandler))
                .httpBasic(withDefaults())
                .sessionManagement((session) -> session
                        .invalidSessionUrl("/login")
                        .maximumSessions(2)
                        .expiredUrl("/login")
                        .sessionRegistry(sessionRegistry));

        authenticationProvider(http, basicUserService, ldapUserService, passwordEncoder);

    }

    /**
     * 套用自訂登入頁面的安全設定。
     *
     * <p>使用 {@code security.login-uri} 所指定的頁面作為登入頁，
     * 並以 {@link SessionCreationPolicy#STATELESS} 管理 Session，適用於前後端分離或 Token 驗證場景。</p>
     *
     * @param http                 {@link HttpSecurity} 設定建構器
     * @param sessionRegistry      Session 登錄表（容器 Bean）
     * @param loginSuccessHandler  登入成功處理器（容器 Bean）
     * @param loginFailureHandler  登入失敗處理器（容器 Bean）
     * @param logoutSuccessHandler 登出成功處理器（容器 Bean）
     * @param basicUserService     BASIC 模式 {@link BasicUserServiceImpl}（容器 Bean）
     * @param ldapUserService      LDAP 模式 {@link LdapUserDetailsService}（容器 Bean）
     * @param passwordEncoder      密碼編碼器（容器 Bean）
     * @throws Exception 設定過程發生例外時拋出
     */
    private void customLoginConfigure(HttpSecurity http,
                                      SessionRegistry sessionRegistry,
                                      LoginSuccessHandler loginSuccessHandler,
                                      LoginFailureHandler loginFailureHandler,
                                      LogoutSuccessHandler logoutSuccessHandler,
                                      BasicUserServiceImpl basicUserService,
                                      LdapUserDetailsService ldapUserService,
                                      PasswordEncoder passwordEncoder) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(switchSecurity()).permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(formLogin -> formLogin.loginPage(securityPropertyConfig.getLoginUri()).permitAll()
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler))
                .logout(logout -> logout
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .permitAll()
                        .logoutSuccessHandler(logoutSuccessHandler))
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .invalidSessionUrl(securityPropertyConfig.getLoginUri())
                        .maximumSessions(2)
                        .expiredUrl(securityPropertyConfig.getLoginUri())
                        .sessionRegistry(sessionRegistry));

        authenticationProvider(http, basicUserService, ldapUserService, passwordEncoder);
    }

    /**
     * 依設定決定 CSRF、X-Frame-Options 防護及認證提供者。
     *
     * <p>依 {@code security.verification-type} 選擇認證提供者：
     * <ul>
     *   <li>{@code LDAP}：使用 {@link LdapUserDetailsService} 進行 LDAP 目錄驗證</li>
     *   <li>{@code CUSTOM}：從 Spring 容器取得自訂 {@link AuthenticationProvider}（需指定 {@code custom-bean-name}）</li>
     *   <li>{@code BASIC}（預設）：使用 {@link DaoAuthenticationProvider} 搭配資料庫使用者資料</li>
     * </ul>
     * </p>
     *
     * @param http             {@link HttpSecurity} 設定建構器
     * @param basicUserService BASIC 模式 {@link BasicUserServiceImpl}（容器 Bean）
     * @param ldapUserService  LDAP 模式 {@link LdapUserDetailsService}（容器 Bean）
     * @param passwordEncoder  密碼編碼器（容器 Bean）
     * @throws Exception 設定過程發生例外，或 CUSTOM 模式未設定 Bean 名稱時拋出
     */
    private void authenticationProvider(HttpSecurity http,
                                        BasicUserServiceImpl basicUserService,
                                        LdapUserDetailsService ldapUserService,
                                        PasswordEncoder passwordEncoder) throws Exception {
        // 依設定套用 X-Frame-Options（預設 SAMEORIGIN，保留點擊劫持防護）
        configureFrameOptions(http);
        // 關閉 csrf 功能
        if (Boolean.FALSE.equals(securityPropertyConfig.getCsrfEnabled())) {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        http.authenticationProvider(
                resolveAuthenticationProvider(basicUserService, ldapUserService, passwordEncoder));
    }

    /**
     * 依 {@code security.verification-type} 解析並回傳對應的 {@link AuthenticationProvider}。
     *
     * <p>此方法同時供表單登入 filter chain 與 JWT 模式的 {@code AuthenticationManager} 重用，
     * 確保兩種狀態策略採用一致的憑證來源：</p>
     * <ul>
     *   <li>{@code LDAP}：回傳 {@link LdapUserDetailsService}</li>
     *   <li>{@code CUSTOM}：回傳 {@code custom-bean-name} 指定的 {@link AuthenticationProvider}（未設定則拋例外）</li>
     *   <li>{@code BASIC}（預設、或未設定）：以 {@link DaoAuthenticationProvider} 搭配 {@link BasicUserServiceImpl}</li>
     * </ul>
     *
     * @param basicUserService BASIC 模式 {@link BasicUserServiceImpl}（容器 Bean）
     * @param ldapUserService  LDAP 模式 {@link LdapUserDetailsService}（容器 Bean）
     * @param passwordEncoder  密碼編碼器（容器 Bean）
     * @return 對應驗證模式的 {@link AuthenticationProvider}
     */
    private AuthenticationProvider resolveAuthenticationProvider(BasicUserServiceImpl basicUserService,
                                                                 LdapUserDetailsService ldapUserService,
                                                                 PasswordEncoder passwordEncoder) {
        VerificationTypeEnum verificationTypeEnum =
                VerificationTypeEnum.getEnum(securityPropertyConfig.getVerificationType());
        log.info("登入模式:{}", verificationTypeEnum == null ? "BASIC(預設)" : verificationTypeEnum.name());

        if (verificationTypeEnum == VerificationTypeEnum.LDAP) {
            return ldapUserService;
        }
        if (verificationTypeEnum == VerificationTypeEnum.CUSTOM) {
            if (StringUtils.isBlank(securityPropertyConfig.getCustomBeanName())) {
                throw new NullPointerException("Please enter value in custom-bean-name");
            }
            return (AuthenticationProvider) ApplicationContextHelper.getBean(securityPropertyConfig.getCustomBeanName());
        }
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(basicUserService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * 依 {@code security.frame-options-mode} 設定套用 X-Frame-Options。
     * 預設 SAMEORIGIN；僅在明確設為 DISABLE 時才停用此防護。
     */
    private void configureFrameOptions(HttpSecurity http) throws Exception {
        switch (securityPropertyConfig.getFrameOptionsMode()) {
            case DISABLE:
                log.warn("X-Frame-Options 已停用 (frame-options-mode=DISABLE)，頁面可被任意站台以 iframe 內嵌，存在點擊劫持風險。");
                http.headers(header -> header.frameOptions(FrameOptionsConfig::disable));
                break;
            case DENY:
                http.headers(header -> header.frameOptions(FrameOptionsConfig::deny));
                break;
            case SAMEORIGIN:
            default:
                http.headers(header -> header.frameOptions(FrameOptionsConfig::sameOrigin));
        }
    }

    /**
     * 根據 {@code security.enable} 決定允許免認證存取的 URI 清單。
     *
     * <p>若安全性停用（{@code false}），回傳萬用符號 {@code /**} 開放所有路徑；
     * 否則解析 {@code security.allow-uris} 設定值，以逗號分隔後回傳。</p>
     *
     * @return 允許免認證存取的 URI 字串陣列
     */
    private String[] switchSecurity() {
        String[] allowUris;
        if (Boolean.FALSE.equals(securityPropertyConfig.getEnable())) {
            log.warn("安全性已停用 (security.enable=false)，所有路徑 {} 將開放未經認證存取，請勿用於正式環境。", PERMIT_ALL);
            allowUris = new String[]{PERMIT_ALL};
        } else {
            allowUris = securityPropertyConfig.getAllowUris().split(StringConstant.COMMA);
        }
        return allowUris;
    }

    /**
     * 建立並回傳 BCrypt 密碼編碼器。
     *
     * <p>標註 {@link ConditionalOnMissingBean}，業務專案宣告同型別 {@link PasswordEncoder} Bean 即可覆寫。</p>
     *
     * @return {@link BCryptPasswordEncoder} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 建立並回傳基本（資料庫）使用者服務實作，注入容器中的密碼編碼器 Bean。
     *
     * <p>標註 {@link ConditionalOnMissingBean}，業務專案宣告同型別 Bean 即可覆寫。
     * 密碼編碼器以方法參數注入，確保取得容器中的 Bean（含使用者覆寫版）。</p>
     *
     * @param passwordEncoder 密碼編碼器（容器 Bean，含使用者覆寫版）
     * @return {@link BasicUserServiceImpl} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    public BasicUserServiceImpl basicUserServiceImpl(PasswordEncoder passwordEncoder) {
        return new BasicUserServiceImpl(passwordEncoder);
    }

    /**
     * 建立並回傳 LDAP 使用者詳細資料服務，注入容器中的密碼編碼器 Bean 與 Security 設定屬性。
     *
     * <p>標註 {@link ConditionalOnMissingBean}，業務專案宣告同型別 Bean 即可覆寫。
     * 密碼編碼器以方法參數注入，確保取得容器中的 Bean（含使用者覆寫版）。</p>
     *
     * @param passwordEncoder 密碼編碼器（容器 Bean，含使用者覆寫版）
     * @return {@link LdapUserDetailsService} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    public LdapUserDetailsService ldapUserDetailsService(PasswordEncoder passwordEncoder) {
        return new LdapUserDetailsService(passwordEncoder, securityPropertyConfig);
    }

    /**
     * 建立並回傳 Session 登錄表，用於追蹤目前有效的 Session 與使用者對應關係。
     *
     * <p>標註 {@link ConditionalOnMissingBean}，業務專案宣告同型別 Bean 即可覆寫。</p>
     *
     * @return {@link SessionRegistryImpl} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * 建立並回傳登入成功處理器，依設定執行成功後的導向或後處理邏輯。
     *
     * <p>標註 {@link ConditionalOnMissingBean}，業務專案宣告同型別 Bean 即可覆寫。</p>
     *
     * @return {@link LoginSuccessHandler} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    public LoginSuccessHandler loginSuccessHandler() {
        return new LoginSuccessHandler(securityPropertyConfig);
    }

    /**
     * 建立並回傳登入失敗處理器，依設定執行失敗後的導向或錯誤記錄邏輯。
     *
     * <p>標註 {@link ConditionalOnMissingBean}，業務專案宣告同型別 Bean 即可覆寫。</p>
     *
     * @return {@link LoginFailureHandler} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    public LoginFailureHandler loginFailureHandler() {
        return new LoginFailureHandler(securityPropertyConfig);
    }

    /**
     * 建立並回傳登出成功處理器，依設定執行登出後的導向邏輯。
     *
     * <p>標註 {@link ConditionalOnMissingBean}，業務專案宣告同型別 Bean 即可覆寫。</p>
     *
     * @return {@link LogoutSuccessHandler} 實例
     */
    @Bean
    @ConditionalOnMissingBean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new LogoutSuccessHandler(securityPropertyConfig);
    }
}
