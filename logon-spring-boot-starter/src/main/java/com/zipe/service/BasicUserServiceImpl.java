package com.zipe.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 基本使用者服務實作，提供 Spring Security 所需的 {@link UserDetailsService} 預設實作。
 * <p>
 * 此類別為開發／測試用的簡易實作，帳號與密碼均硬編碼為 {@code admin}。
 * 正式環境應繼承或替換此類別，改由資料庫或 LDAP 等外部來源查詢使用者資訊。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/19 下午 05:12
 **/
public class BasicUserServiceImpl implements UserDetailsService {

    /** 密碼編碼器，用於對明文密碼進行雜湊處理後再交給 Spring Security 比對 */
    private final PasswordEncoder passwordEncoder;

    /**
     * 建構子，透過 Spring 依賴注入取得 {@link PasswordEncoder}。
     *
     * @param passwordEncoder Spring Security 的密碼編碼器實例
     */
    @Autowired
    public BasicUserServiceImpl(PasswordEncoder passwordEncoder){
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 依使用者名稱載入使用者詳細資訊，供 Spring Security 進行驗證。
     * <p>
     * 此預設實作僅接受帳號 {@code admin}；其他帳號一律拋出
     * {@link UsernameNotFoundException}。
     * </p>
     *
     * @param username 使用者輸入的帳號名稱
     * @return 包含帳號、密碼與權限的 {@link UserDetails} 物件
     * @throws UsernameNotFoundException 當使用者名稱不存在時拋出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 簡單起見，直接內部校驗
        String uname = "admin";
        String passwd = "admin";

        if (!username.equals(uname)) {
            throw new UsernameNotFoundException(username);
        }
        // 封裝成 Spring Security 定義的 UserDetails 物件後回傳
        // 須同時設定 password 與 passwordEncoder，build() 會以 encoder 對 password 編碼
        return User.builder()
                .username(username)
                .password(passwd)
                .passwordEncoder(passwordEncoder::encode)
                .authorities(new SimpleGrantedAuthority("admin"))
                .build();
    }
}
