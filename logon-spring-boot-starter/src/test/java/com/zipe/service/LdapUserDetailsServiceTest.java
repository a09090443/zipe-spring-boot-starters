package com.zipe.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 驗證 LDAP 認證成功後產生的 Authentication token 設定正確。
 */
class LdapUserDetailsServiceTest {

    /**
     * 認證成功的 token 必須：authorities 非 null、為已認證狀態、
     * 且不得保留明文密碼（credentials 抹除為 null）。
     */
    @Test
    void buildAuthenticatedTokenHasNonNullAuthoritiesAndNoCredentials() {
        UsernamePasswordAuthenticationToken token =
                LdapUserDetailsService.buildAuthenticatedToken("alice");

        assertEquals("alice", token.getPrincipal());
        assertNotNull(token.getAuthorities(), "authorities 不可為 null");
        assertTrue(token.isAuthenticated(), "應為已認證狀態");
        assertNull(token.getCredentials(), "不得保留明文密碼");
    }
}
