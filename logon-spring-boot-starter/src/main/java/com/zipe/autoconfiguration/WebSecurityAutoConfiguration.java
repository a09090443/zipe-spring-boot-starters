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
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/19 下午 04:16
 **/
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({SecurityPropertyConfig.class})
public class WebSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    private final SecurityPropertyConfig securityPropertyConfig;

    WebSecurityAutoConfiguration(SecurityPropertyConfig securityPropertyConfig) {
        if (!securityPropertyConfig.getEnable()) {
            securityPropertyConfig.setAllowUris("/**");
        }
        this.securityPropertyConfig = securityPropertyConfig;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if (StringUtils.isNotBlank(securityPropertyConfig.getLoginUri())) {
            customLoginConfigure(http);
        } else {
            basicLoginConfigure(http);
        }
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth)
            throws Exception {

        VerificationTypeEnum verificationTypeEnum = VerificationTypeEnum.getEnum(securityPropertyConfig.getVerificationType());
        switch (verificationTypeEnum) {
            case LDAP:
                auth.authenticationProvider(ldapUserDetailsService());
                break;
            case CUSTOM:
                auth.authenticationProvider((AuthenticationProvider) ApplicationContextHelper.getBean(securityPropertyConfig.getCustomBeanName()));
                break;
            case BASIC:
            default:
                auth.userDetailsService(basicUserServiceImpl()).passwordEncoder(passwordEncoder());
                break;
        }
    }

    private void basicLoginConfigure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers(securityPropertyConfig.getAllowUris().split(StringConstant.COMMA)).permitAll()
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
    }

    private void customLoginConfigure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers(securityPropertyConfig.getAllowUris().split(StringConstant.COMMA)).permitAll()
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
