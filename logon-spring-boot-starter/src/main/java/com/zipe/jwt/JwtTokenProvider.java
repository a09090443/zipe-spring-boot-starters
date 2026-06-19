package com.zipe.jwt;

import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
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
        String algorithm = properties.getAlgorithm();
        if ("HS256".equalsIgnoreCase(algorithm)) {
            if (StringUtils.isBlank(properties.getSecret())) {
                throw new IllegalStateException("security.jwt.secret is required for HS256");
            }
            SecretKey key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
            this.signKey = key;
            this.verifyKey = key;
        } else if ("RS256".equalsIgnoreCase(algorithm)) {
            if (StringUtils.isBlank(properties.getPrivateKeyLocation())
                    || StringUtils.isBlank(properties.getPublicKeyLocation())) {
                throw new IllegalStateException(
                        "security.jwt.private-key-location and public-key-location are required for RS256");
            }
            try {
                this.signKey = loadPrivateKey(properties.getPrivateKeyLocation());
                this.verifyKey = loadPublicKey(properties.getPublicKeyLocation());
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to load RS256 keys: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalStateException("Unsupported algorithm: " + algorithm);
        }
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getExpirationSeconds());
        var builder = Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp));
        // 明確指定設定的演算法，避免 jjwt 依金鑰長度自動改用更強的 HS384/HS512，
        // 確保 token header 的 alg 與 security.jwt.algorithm 設定一致。
        if (signKey instanceof SecretKey secretKey) {
            builder.signWith(secretKey, Jwts.SIG.HS256);
        } else {
            builder.signWith((PrivateKey) signKey, Jwts.SIG.RS256);
        }
        if (StringUtils.isNotBlank(properties.getIssuer())) {
            builder.issuer(properties.getIssuer());
        }
        return builder.compact();
    }

    public String validateAndGetUsername(String token) {
        JwtParserBuilder parserBuilder = Jwts.parser();
        if (verifyKey instanceof SecretKey secretKey) {
            parserBuilder.verifyWith(secretKey);
        } else {
            parserBuilder.verifyWith((PublicKey) verifyKey);
        }
        return parserBuilder
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private PrivateKey loadPrivateKey(String location) throws Exception {
        byte[] der = readPemDer(location, "PRIVATE KEY");
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private PublicKey loadPublicKey(String location) throws Exception {
        byte[] der = readPemDer(location, "PUBLIC KEY");
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private byte[] readPemDer(String location, String type) throws Exception {
        var resource = new DefaultResourceLoader().getResource(location);
        String pem;
        try (var in = resource.getInputStream()) {
            pem = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
        String base64 = pem.replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
