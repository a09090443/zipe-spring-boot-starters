package com.zipe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zipe.config.BasicUser;
import com.zipe.config.SecurityPropertyConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 驗證 {@link BasicUserServiceImpl} 在 {@code security.basic.users} 設定下的帳號載入行為，
 * 以及未設定時 fallback 回內建 {@code admin/admin} 的向後相容行為。
 */
class BasicUserServiceImplTest {

    /** 與正式環境一致的委派式編碼器，能依 {@code {id}} 前綴比對並對明文編碼。 */
    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private SecurityPropertyConfig configWithUsers(List<BasicUser> users) {
        SecurityPropertyConfig config = new SecurityPropertyConfig();
        config.getBasic().setUsers(users);
        return config;
    }

    private BasicUser user(String username, String password, List<String> authorities) {
        BasicUser u = new BasicUser();
        u.setUsername(username);
        u.setPassword(password);
        u.setAuthorities(authorities);
        return u;
    }

    /**
     * 設定明文密碼的帳號，載入後密碼須能以委派式編碼器比對成功，且權限正確注入。
     */
    @Test
    void loadsConfiguredUserWithPlaintextPassword() {
        SecurityPropertyConfig config = configWithUsers(
                List.of(user("user01", "1234", List.of("admin", "viewer"))));
        BasicUserServiceImpl service = new BasicUserServiceImpl(encoder, config);

        UserDetails details = service.loadUserByUsername("user01");

        assertEquals("user01", details.getUsername());
        assertTrue(encoder.matches("1234", details.getPassword()), "明文密碼應可比對成功");
        assertTrue(details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("admin"::equals),
                "應含 admin 權限");
        assertTrue(details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("viewer"::equals),
                "應含 viewer 權限");
    }

    /**
     * 設定 {@code {bcrypt}} 前綴的預雜湊密碼，載入後原值採用、原明文須能比對成功。
     */
    @Test
    void loadsConfiguredUserWithPreHashedPassword() {
        String hashed = encoder.encode("secret");
        SecurityPropertyConfig config = configWithUsers(
                List.of(user("user02", hashed, List.of("viewer"))));
        BasicUserServiceImpl service = new BasicUserServiceImpl(encoder, config);

        UserDetails details = service.loadUserByUsername("user02");

        assertEquals(hashed, details.getPassword(), "預雜湊密碼應原值採用");
        assertTrue(encoder.matches("secret", details.getPassword()), "原明文應可比對成功");
    }

    /**
     * 有設定 users 但查無對應帳號時，須拋出 {@link UsernameNotFoundException}。
     */
    @Test
    void throwsWhenConfiguredButUsernameNotFound() {
        SecurityPropertyConfig config = configWithUsers(
                List.of(user("user01", "1234", List.of())));
        BasicUserServiceImpl service = new BasicUserServiceImpl(encoder, config);

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nobody"));
    }

    /**
     * 未設定 users 時，fallback 回內建 {@code admin/admin}（向後相容）。
     */
    @Test
    void fallsBackToAdminWhenNoUsersConfigured() {
        BasicUserServiceImpl service = new BasicUserServiceImpl(encoder, new SecurityPropertyConfig());

        UserDetails details = service.loadUserByUsername("admin");

        assertEquals("admin", details.getUsername());
        assertTrue(encoder.matches("admin", details.getPassword()), "admin/admin 應可比對成功");
        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("user01"));
    }
}
