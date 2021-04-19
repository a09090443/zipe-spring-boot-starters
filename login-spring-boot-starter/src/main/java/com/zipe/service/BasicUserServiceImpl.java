package com.zipe.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/19 下午 05:12
 **/
public class BasicUserServiceImpl implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public BasicUserServiceImpl(PasswordEncoder passwordEncoder){
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 簡單起見，直接內部校驗
        String uname = "admin";
        String passwd = "admin";

        if (!username.equals(uname)) {
            throw new UsernameNotFoundException(username);
        }
        // 封裝成 Spring security 定義的 User 對象
        return User.builder()
                .username(username)
                .passwordEncoder(s -> passwordEncoder.encode(passwd))
                .authorities(new SimpleGrantedAuthority("admin"))
                .build();
    }
}
