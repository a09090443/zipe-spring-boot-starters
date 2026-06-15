package com.zipe.jwt;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    private JwtProperties hs256Props() {
        JwtProperties p = new JwtProperties();
        p.setAlgorithm("HS256");
        // 32 bytes 以上，滿足 HS256 金鑰長度需求
        p.setSecret("0123456789-0123456789-0123456789-secret");
        p.setExpirationSeconds(3600L);
        return p;
    }

    @Test
    void hs256_generateThenValidate_shouldRoundTrip() {
        JwtTokenProvider provider = new JwtTokenProvider(hs256Props());
        provider.init();
        String token = provider.generateToken("alice");
        assertEquals("alice", provider.validateAndGetUsername(token));
    }

    @Test
    void hs256_tamperedToken_shouldThrow() {
        JwtTokenProvider provider = new JwtTokenProvider(hs256Props());
        provider.init();
        String token = provider.generateToken("alice");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(JwtException.class, () -> provider.validateAndGetUsername(tampered));
    }

    @Test
    void hs256_expiredToken_shouldThrow() {
        JwtProperties p = hs256Props();
        p.setExpirationSeconds(-1L); // 立即過期
        JwtTokenProvider provider = new JwtTokenProvider(p);
        provider.init();
        String token = provider.generateToken("alice");
        assertThrows(JwtException.class, () -> provider.validateAndGetUsername(token));
    }
}
