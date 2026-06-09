package com.zipe.autoconfiguration;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.enums.VerificationTypeEnum;
import com.zipe.handler.LoginFailureHandler;
import com.zipe.handler.LoginSuccessHandler;
import com.zipe.handler.LogoutSuccessHandler;
import com.zipe.service.BasicUserServiceImpl;
import com.zipe.service.LdapUserDetailsService;
import com.zipe.util.ApplicationContextHelper;
import com.zipe.util.string.StringConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
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
@EnableConfigurationProperties({SecurityPropertyConfig.class})
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

    /**
     * 建立並回傳主要的 {@link SecurityFilterChain}。
     *
     * <p>若設定了自訂登入頁 URI（{@code security.login-uri}），則套用自訂登入流程；
     * 否則使用 Spring Security 預設表單登入流程。</p>
     *
     * @param http {@link HttpSecurity} 設定建構器
     * @return 建置完成的 {@link SecurityFilterChain}
     * @throws Exception 設定過程發生例外時拋出
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (StringUtils.isNotBlank(securityPropertyConfig.getLoginUri())) {
            customLoginConfigure(http);
        } else {
            basicLoginConfigure(http);
        }
        return http.build();
    }

    /**
     * 套用 Spring Security 預設表單登入的安全設定。
     *
     * <p>使用內建登入頁面，並設定登入成功/失敗處理器、登出行為與 Session 管理。</p>
     *
     * @param http {@link HttpSecurity} 設定建構器
     * @throws Exception 設定過程發生例外時拋出
     */
    private void basicLoginConfigure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(switchSecurity()).permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(formLogin -> formLogin.permitAll()
                        .successHandler(loginSuccessHandler())
                        .failureHandler(loginFailureHandler()))
                .logout(logout -> logout.logoutUrl("/login")
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .permitAll()
                        .logoutSuccessHandler(logoutSuccessHandler()))
                .httpBasic(withDefaults())
                .sessionManagement((session) -> session
                        .invalidSessionUrl("/login")
                        .maximumSessions(2)
                        .expiredUrl("/login")
                        .sessionRegistry(sessionRegistry()));

        authenticationProvider(http);

    }

    /**
     * 套用自訂登入頁面的安全設定。
     *
     * <p>使用 {@code security.login-uri} 所指定的頁面作為登入頁，
     * 並以 {@link SessionCreationPolicy#STATELESS} 管理 Session，適用於前後端分離或 Token 驗證場景。</p>
     *
     * @param http {@link HttpSecurity} 設定建構器
     * @throws Exception 設定過程發生例外時拋出
     */
    private void customLoginConfigure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(switchSecurity()).permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(formLogin -> formLogin.loginPage(securityPropertyConfig.getLoginUri()).permitAll()
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler())
                        .failureHandler(loginFailureHandler()))
                .logout(logout -> logout
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .permitAll()
                        .logoutSuccessHandler(logoutSuccessHandler()))
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .invalidSessionUrl(securityPropertyConfig.getLoginUri())
                        .maximumSessions(2)
                        .expiredUrl(securityPropertyConfig.getLoginUri())
                        .sessionRegistry(sessionRegistry()));

        authenticationProvider(http);
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
     * @param http {@link HttpSecurity} 設定建構器
     * @throws Exception 設定過程發生例外，或 CUSTOM 模式未設定 Bean 名稱時拋出
     */
    private void authenticationProvider(HttpSecurity http) throws Exception {
        // 依設定套用 X-Frame-Options（預設 SAMEORIGIN，保留點擊劫持防護）
        configureFrameOptions(http);
        // 關閉 csrf 功能
        if (Boolean.FALSE.equals(securityPropertyConfig.getCsrfEnabled())) {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        VerificationTypeEnum verificationTypeEnum = VerificationTypeEnum.getEnum(securityPropertyConfig.getVerificationType());
        log.info("登入模式:{}", verificationTypeEnum.name());

        switch (verificationTypeEnum) {
            case LDAP:
                http.authenticationProvider(ldapUserDetailsService());
                break;
            case CUSTOM:
                if (StringUtils.isBlank(securityPropertyConfig.getCustomBeanName())) {
                    throw new NullPointerException("Please enter value in custom-bean-name");
                }
                http.authenticationProvider((AuthenticationProvider) ApplicationContextHelper.getBean(securityPropertyConfig.getCustomBeanName()));
                break;
            case BASIC:
            default:
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(basicUserServiceImpl());
                authProvider.setPasswordEncoder(passwordEncoder());
                http.authenticationProvider(authProvider);
        }
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
     * @return {@link BCryptPasswordEncoder} 實例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 建立並回傳基本（資料庫）使用者服務實作，注入 BCrypt 密碼編碼器。
     *
     * @return {@link BasicUserServiceImpl} 實例
     */
    @Bean
    public BasicUserServiceImpl basicUserServiceImpl() {
        return new BasicUserServiceImpl(this.passwordEncoder());
    }

    /**
     * 建立並回傳 LDAP 使用者詳細資料服務，注入密碼編碼器與 Security 設定屬性。
     *
     * @return {@link LdapUserDetailsService} 實例
     */
    @Bean
    public LdapUserDetailsService ldapUserDetailsService() {
        return new LdapUserDetailsService(this.passwordEncoder(), securityPropertyConfig);
    }

    /**
     * 建立並回傳 Session 登錄表，用於追蹤目前有效的 Session 與使用者對應關係。
     *
     * @return {@link SessionRegistryImpl} 實例
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * 建立並回傳登入成功處理器，依設定執行成功後的導向或後處理邏輯。
     *
     * @return {@link LoginSuccessHandler} 實例
     */
    @Bean
    public LoginSuccessHandler loginSuccessHandler() {
        return new LoginSuccessHandler(securityPropertyConfig);
    }

    /**
     * 建立並回傳登入失敗處理器，依設定執行失敗後的導向或錯誤記錄邏輯。
     *
     * @return {@link LoginFailureHandler} 實例
     */
    @Bean
    public LoginFailureHandler loginFailureHandler() {
        return new LoginFailureHandler(securityPropertyConfig);
    }

    /**
     * 建立並回傳登出成功處理器，依設定執行登出後的導向邏輯。
     *
     * @return {@link LogoutSuccessHandler} 實例
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new LogoutSuccessHandler(securityPropertyConfig);
    }
}
