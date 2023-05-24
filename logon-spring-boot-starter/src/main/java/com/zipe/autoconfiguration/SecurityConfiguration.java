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
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author Gary Tsai
 */
@Slf4j
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({SecurityPropertyConfig.class})
public class SecurityConfiguration {

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
        String[] matchUri = securityPropertyConfig.getAllowUris().split(StringConstant.COMMA);
        if (Boolean.FALSE.equals(securityPropertyConfig.getEnable())) {
            matchUri = new String[]{"/**"};
        }

        http.authorizeHttpRequests()
                .requestMatchers(matchUri).permitAll()
                .anyRequest()
                .authenticated()
                .and().formLogin()
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .and()
                .logout()
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .permitAll()
                .logoutSuccessHandler(logoutSuccessHandler())
                .and()
                .httpBasic()
                .and().sessionManagement().invalidSessionUrl("/login")
                .maximumSessions(2).expiredUrl("/login").sessionRegistry(sessionRegistry());
        // 關閉 iframe 阻擋
        http.headers().frameOptions().disable();
        // 關閉 csrf 功能
        if (!securityPropertyConfig.getCsrfEnabled()) {
            http.csrf().disable();
        }
        authenticationProvider(http);

    }

    private void customLoginConfigure(HttpSecurity http) throws Exception {

        String[] matchUri = securityPropertyConfig.getAllowUris().split(StringConstant.COMMA);
        if (Boolean.FALSE.equals(securityPropertyConfig.getEnable())) {
            matchUri = new String[]{"/**"};
        }
        http
                .csrf().disable()
                .authorizeHttpRequests ()
                .requestMatchers(matchUri).permitAll()
                .anyRequest()
                .authenticated()
                .and().formLogin()
                .loginPage(securityPropertyConfig.getLoginUri())
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .and()
                .logout()
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .permitAll()
                .logoutSuccessHandler(logoutSuccessHandler())
                .and().sessionManagement().invalidSessionUrl(securityPropertyConfig.getLoginUri())
                .maximumSessions(2).expiredUrl(securityPropertyConfig.getLoginUri()).sessionRegistry(sessionRegistry());
        // 關閉 iframe 阻擋
        http.headers().frameOptions().disable();
        // 關閉 csrf 功能
        if (!securityPropertyConfig.getCsrfEnabled()) {
            http.csrf().disable();
        }
        authenticationProvider(http);
    }

    private void authenticationProvider(HttpSecurity http){
        VerificationTypeEnum verificationTypeEnum = VerificationTypeEnum.getEnum(securityPropertyConfig.getVerificationType());
        log.info("登入模式:{}", verificationTypeEnum.name());

        switch (verificationTypeEnum) {
            case LDAP -> http.authenticationProvider(ldapUserDetailsService());
            case CUSTOM -> {
                if (StringUtils.isBlank(securityPropertyConfig.getCustomBeanName())) {
                    throw new NullPointerException("Please enter value in custom-bean-name");
                }
                http.authenticationProvider(
                    (AuthenticationProvider) ApplicationContextHelper.getBean(
                        securityPropertyConfig.getCustomBeanName()));
            }
            default -> {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(basicUserServiceImpl());
                authProvider.setPasswordEncoder(passwordEncoder());
                http.authenticationProvider(authProvider);
            }
        }
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
