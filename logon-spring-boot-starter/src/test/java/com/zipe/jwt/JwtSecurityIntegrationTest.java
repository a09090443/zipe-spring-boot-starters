package com.zipe.jwt;

import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zipe.jwt.it.JwtItApplication;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * JWT 登入模式端到端整合測試：登入取 token → 帶 token 存取受保護資源 → 不帶 token 回 401；
 * 另驗證錯誤簽章與已過期的 token 經實際 {@code SecurityFilterChain} 皆被拒且不建立已認證 principal。
 * <p>斷言與輸出皆不印出 token 值。</p>
 */
@SpringBootTest(
        classes = JwtItApplication.class,
        properties = {
                "security.verification-type=basic",
                "security.jwt.enabled=true",
                "security.jwt.secret=" + JwtSecurityIntegrationTest.SECRET,
                "security.allow-uris=/public/**",
                "security.login-uri="
        })
@AutoConfigureMockMvc
class JwtSecurityIntegrationTest {

    /** 測試用 HS256 密鑰（僅測試範圍）。 */
    static final String SECRET = "0123456789-0123456789-0123456789-secret";

    /** 與 {@link #SECRET} 不同的測試密鑰，用於產生簽章不符的 token。 */
    private static final String OTHER_SECRET = "9876543210-9876543210-9876543210-other!";

    @Autowired
    MockMvc mockMvc;

    @Test
    void login_then_access_protected_resource() throws Exception {
        String body = mockMvc.perform(post("/api/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        String token = JsonMapper.builder().build().readTree(body).get("token").asText();

        mockMvc.perform(get("/whoami").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));

        mockMvc.perform(get("/whoami"))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    /** 以不同密鑰簽署的 token：簽章驗證失敗，回 401 且未建立 principal。 */
    @Test
    void token_signed_with_other_key_is_rejected() throws Exception {
        Instant now = Instant.now();
        String forged = sign(OTHER_SECRET, now, now.plusSeconds(600));

        mockMvc.perform(get("/whoami").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    /** 簽章正確但已過期的 token：回 401 且未建立 principal。 */
    @Test
    void expired_token_is_rejected() throws Exception {
        Instant now = Instant.now();
        String expired = sign(SECRET, now.minusSeconds(3600), now.minusSeconds(1800));

        mockMvc.perform(get("/whoami").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    /** 產生 subject 為 admin 的 HS256 token；僅用於測試，不修改正式的 {@link JwtTokenProvider}。 */
    private static String sign(String secret, Instant issuedAt, Instant expiration) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("admin")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
