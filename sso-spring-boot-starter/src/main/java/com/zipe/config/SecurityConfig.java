package com.zipe.config;

import com.zipe.service.LdapUserDetailsService;
import com.zipe.service.LoginFailureHandler;
import com.zipe.service.LoginSuccessHandler;
import com.zipe.util.string.StringConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.Objects;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final Environment env;

    private final LdapUserDetailsService ldapUserDetailsService;

    private final LoginSuccessHandler loginSuccessHandler;

    private final LoginFailureHandler loginFailureHandler;

    private final LogoutSuccessHandler logoutSuccessHandler;

    private final SecurityPropertyConfig securityPropertyConfig;

    @Autowired
    SecurityConfig(Environment env,
                   LdapUserDetailsService ldapUserDetailsService,
                   LoginSuccessHandler loginSuccessHandler,
                   LogoutSuccessHandler logoutSuccessHandler,
                   LoginFailureHandler loginFailureHandler,
                   SecurityPropertyConfig securityPropertyConfig) {
        this.env = env;
        this.ldapUserDetailsService = ldapUserDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.securityPropertyConfig = securityPropertyConfig;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();

        http.authorizeRequests()
                .antMatchers(securityPropertyConfig.getAllowUris()).permitAll()
                .anyRequest().authenticated();

        if (Objects.isNull(securityPropertyConfig.getLoginUri())) {
            // Spring Security 預設 login 畫面
            http.formLogin().and().httpBasic();
        } else {
            // 自訂 login 畫面
            http.formLogin()
                    .loginPage(securityPropertyConfig.getLoginUri())
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .permitAll()
                    .successHandler(loginSuccessHandler)
                    .failureHandler(loginFailureHandler)
                    .and()
                    .logout()
                    .deleteCookies("JSESSIONID")
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .permitAll()
                    .logoutSuccessHandler(logoutSuccessHandler)
                    .and()
                    .sessionManagement().invalidSessionUrl(securityPropertyConfig.getLoginUri())
                    .maximumSessions(2).expiredUrl(securityPropertyConfig.getLoginUri()).sessionRegistry(sessionRegistry());
        }
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        if (Objects.equals(env.getProperty("ldap.enabled"), StringConstant.TRUE.toLowerCase())) {
            auth.authenticationProvider(ldapUserDetailsService);
        } else {
			auth.inMemoryAuthentication().withUser("admin").password("admin").roles("ADMIN");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationTrustResolver getAuthenticationTrustResolver() {
        return new AuthenticationTrustResolverImpl();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new SessionListener();
    }

}
