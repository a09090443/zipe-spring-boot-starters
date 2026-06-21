package com.zipe.service;

import com.zipe.config.BasicUser;
import com.zipe.config.SecurityPropertyConfig;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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

    /** Security 設定屬性，提供 {@code security.basic.users} 自訂帳號來源；可為 {@code null}。 */
    private final SecurityPropertyConfig securityPropertyConfig;

    /**
     * 建構子，透過 Spring 依賴注入取得 {@link PasswordEncoder}。
     *
     * <p>此建構子不帶設定來源，使用者一律 fallback 至內建 {@code admin/admin}，
     * 保留供既有呼叫端與測試使用。</p>
     *
     * @param passwordEncoder Spring Security 的密碼編碼器實例
     */
    public BasicUserServiceImpl(PasswordEncoder passwordEncoder) {
        this(passwordEncoder, null);
    }

    /**
     * 建構子，注入密碼編碼器與 Security 設定屬性。
     *
     * <p>當 {@code security.basic.users} 有設定時，改由該清單載入帳號；未設定時 fallback 回
     * 內建 {@code admin/admin}（向後相容）。</p>
     *
     * @param passwordEncoder        Spring Security 的密碼編碼器實例
     * @param securityPropertyConfig Security 設定屬性（可為 {@code null}，等同未設定自訂帳號）
     */
    @Autowired
    public BasicUserServiceImpl(PasswordEncoder passwordEncoder,
                                SecurityPropertyConfig securityPropertyConfig) {
        this.passwordEncoder = passwordEncoder;
        this.securityPropertyConfig = securityPropertyConfig;
    }

    /**
     * 依使用者名稱載入使用者詳細資訊，供 Spring Security 進行驗證。
     * <p>
     * 當 {@code security.basic.users} 有設定帳號時，由該清單比對載入；查無對應帳號拋出
     * {@link UsernameNotFoundException}。未設定任何帳號時 fallback 回內建 {@code admin/admin}
     * （開發／測試用，正式環境請以設定檔自訂帳號或覆寫本 Bean）。
     * </p>
     *
     * @param username 使用者輸入的帳號名稱
     * @return 包含帳號、密碼與權限的 {@link UserDetails} 物件
     * @throws UsernameNotFoundException 當使用者名稱不存在時拋出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<BasicUser> users = configuredUsers();
        if (users.isEmpty()) {
            return loadFallbackAdmin(username);
        }
        return users.stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst()
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    /**
     * 取得設定檔中的使用者清單，{@code securityPropertyConfig} 為 {@code null} 時回傳空清單。
     */
    private List<BasicUser> configuredUsers() {
        if (securityPropertyConfig == null || securityPropertyConfig.getBasic() == null) {
            return Collections.emptyList();
        }
        List<BasicUser> users = securityPropertyConfig.getBasic().getUsers();
        return users == null ? Collections.emptyList() : users;
    }

    /**
     * 將設定的 {@link BasicUser} 轉為 {@link UserDetails}。
     *
     * <p>密碼以 {@code {} 開頭視為已含 {@code {id}} 前綴的雜湊值，原值採用，交由
     * {@code DelegatingPasswordEncoder} 依前綴比對；否則視為明文，以 {@link PasswordEncoder}
     * 編碼後採用。</p>
     */
    private UserDetails toUserDetails(BasicUser user) {
        String rawPassword = user.getPassword();
        String storedPassword = isEncoded(rawPassword)
                ? rawPassword
                : passwordEncoder.encode(rawPassword);
        List<String> authorities = user.getAuthorities() == null
                ? Collections.emptyList()
                : user.getAuthorities();
        return User.builder()
                .username(user.getUsername())
                .password(storedPassword)
                .authorities(authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 判斷密碼是否已為帶 {@code {id}} 前綴的編碼值（例如 {@code {bcrypt}$2a$...}）。
     */
    private boolean isEncoded(String password) {
        return password != null && password.startsWith("{");
    }

    /**
     * 未設定任何帳號時的 fallback：僅接受內建 {@code admin/admin}。
     */
    private UserDetails loadFallbackAdmin(String username) {
        if (!"admin".equals(username)) {
            throw new UsernameNotFoundException(username);
        }
        // 須同時設定 password 與 passwordEncoder，build() 會以 encoder 對 password 編碼
        return User.builder()
                .username(username)
                .password("admin")
                .passwordEncoder(passwordEncoder::encode)
                .authorities(new SimpleGrantedAuthority("admin"))
                .build();
    }
}
