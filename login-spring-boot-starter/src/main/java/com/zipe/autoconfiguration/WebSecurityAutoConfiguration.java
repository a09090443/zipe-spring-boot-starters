package com.zipe.autoconfiguration;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.service.BasicUserServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/19 下午 04:16
 **/
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityPropertyConfig.class)
@ConditionalOnProperty(prefix = "security", name = "enable", havingValue = "true")
public class WebSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    private final SecurityPropertyConfig securityPropertyConfig;

    WebSecurityAutoConfiguration(SecurityPropertyConfig securityPropertyConfig) {
        this.securityPropertyConfig = securityPropertyConfig;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .anyRequest()
                .authenticated()
                .and().formLogin()
                .and().httpBasic()
                .and().csrf().disable(); // <-- 關閉CSRF，請求時才不用另外帶CSRF token
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth)
            throws Exception {
//        auth
//                .inMemoryAuthentication()
//                .withUser("user")
//                .password("password")
//                .roles("USER")
//                .and()
//                .withUser("admin")
//                .password("{noop}admin")
//                .roles("USER", "ADMIN");

        // 用戶登錄資訊校驗使用自定義 userService
        // 還需要注意密碼加密與驗證需要使用同一種方式
        auth.userDetailsService(basicUserServiceImpl()).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public BasicUserServiceImpl basicUserServiceImpl() {
        return new BasicUserServiceImpl(this.passwordEncoder());
    }
}
