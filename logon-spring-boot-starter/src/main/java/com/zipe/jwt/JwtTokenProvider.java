package com.zipe.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 簽發與驗證核心。依 {@link JwtProperties#getAlgorithm()} 選擇 HS256 / RS256。
 */
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties properties;
    private Key signKey;
    private Key verifyKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if ("HS256".equalsIgnoreCase(properties.getAlgorithm())) {
            if (StringUtils.isBlank(properties.getSecret())) {
                throw new IllegalStateException("security.jwt.secret is required for HS256");
            }
            SecretKey key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
            this.signKey = key;
            this.verifyKey = key;
        } else {
            throw new IllegalStateException("Unsupported algorithm: " + properties.getAlgorithm());
        }
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getExpirationSeconds());
        var builder = Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signKey);
        if (StringUtils.isNotBlank(properties.getIssuer())) {
            builder.issuer(properties.getIssuer());
        }
        return builder.compact();
    }

    public String validateAndGetUsername(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) verifyKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
