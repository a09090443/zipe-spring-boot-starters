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

@Slf4j
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({SecurityPropertyConfig.class})
public class SecurityConfiguration {

    private final String PERMIT_ALL = "/**";
    private final SecurityPropertyConfig securityPropertyConfig;

    public SecurityConfiguration(SecurityPropertyConfig securityPropertyConfig) {
        this.securityPropertyConfig = securityPropertyConfig;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (StringUtils.isNotBlank(securityPropertyConfig.getLoginUri())) {
            customLoginConfigure(http);
        } else {
            basicLoginConfigure(http);
        }
        return http.build();
    }

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public BasicUserServiceImpl basicUserServiceImpl() {
        return new BasicUserServiceImpl(this.passwordEncoder());
    }

    @Bean
    public LdapUserDetailsService ldapUserDetailsService() {
        return new LdapUserDetailsService(this.passwordEncoder(), securityPropertyConfig);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public LoginSuccessHandler loginSuccessHandler() {
        return new LoginSuccessHandler(securityPropertyConfig);
    }

    @Bean
    public LoginFailureHandler loginFailureHandler() {
        return new LoginFailureHandler(securityPropertyConfig);
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new LogoutSuccessHandler(securityPropertyConfig);
    }
}
