package com.zipe.jwt;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void hs256_tokenHeader_shouldUseConfiguredAlgorithm() {
        JwtProperties p = new JwtProperties();
        p.setAlgorithm("HS256");
        // 64 bytes 金鑰：若未明確指定演算法，jjwt 會依金鑰長度自動選用 HS512，
        // 因此本測試確保簽章固定採用設定的 HS256。
        p.setSecret("0123456789012345678901234567890123456789012345678901234567890123");
        JwtTokenProvider provider = new JwtTokenProvider(p);
        provider.init();

        String token = provider.generateToken("alice");
        String headerJson = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);

        assertTrue(headerJson.contains("\"alg\":\"HS256\""),
                "token header 應使用設定的 HS256，實際為: " + headerJson);
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
