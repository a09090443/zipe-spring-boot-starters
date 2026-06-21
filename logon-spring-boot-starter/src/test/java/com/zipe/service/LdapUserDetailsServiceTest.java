package com.zipe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.security.GrantedAuthoritiesResolver;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 驗證 LDAP 認證成功後產生的 Authentication token 設定正確。
 */
class LdapUserDetailsServiceTest {

    /**
     * 認證成功的 token 必須：authorities 非 null、為已認證狀態、
     * 且不得保留明文密碼（credentials 抹除為 null）。
     *
     * <p>使用回傳空集合的預設 {@code GrantedAuthoritiesResolver}，驗證向後相容行為
     * （未引入 iam-starter 時 authorities 為空集合而非 null）。</p>
     */
    @Test
    void buildAuthenticatedTokenHasNonNullAuthoritiesAndNoCredentials() {
        LdapUserDetailsService service = new LdapUserDetailsService(
                null, new SecurityPropertyConfig(), username -> Collections.emptyList());

        UsernamePasswordAuthenticationToken token = service.buildAuthenticatedToken("alice");

        assertEquals("alice", token.getPrincipal());
        assertNotNull(token.getAuthorities(), "authorities 不可為 null");
        assertTrue(token.isAuthenticated(), "應為已認證狀態");
        assertNull(token.getCredentials(), "不得保留明文密碼");
    }

    /**
     * 以 stub 的 {@link GrantedAuthoritiesResolver} 驗證授權注入行為：
     * {@code buildAuthenticatedToken} 應以解析器回傳的 authorities 建立已認證 token，
     * 證明引入 iam-starter 等覆寫解析器後，LDAP 模式可由外部來源補上群組／權限。
     *
     * <p>不需真連 LDAP，僅針對 {@code buildAuthenticatedToken} 的行為。</p>
     */
    @Test
    void buildAuthenticatedTokenInjectsAuthoritiesFromResolver() {
        GrantedAuthoritiesResolver stubResolver = username -> List.<GrantedAuthority>of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("USER_CREATE"));
        LdapUserDetailsService service = new LdapUserDetailsService(
                null, new SecurityPropertyConfig(), stubResolver);

        UsernamePasswordAuthenticationToken token = service.buildAuthenticatedToken("bob");

        assertEquals("bob", token.getPrincipal());
        assertTrue(token.isAuthenticated(), "應為已認證狀態");
        assertEquals(2, token.getAuthorities().size(), "應注入解析器回傳的兩個 authority");
        assertTrue(token.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_ADMIN"::equals),
                "應包含 ROLE_ADMIN");
        assertTrue(token.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("USER_CREATE"::equals),
                "應包含 USER_CREATE");
    }
}
