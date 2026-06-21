package com.zipe.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zipe.jwt.it.JwtItApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * JWT 登入模式端到端整合測試：登入取 token → 帶 token 存取受保護資源 → 不帶 token 回 401。
 */
@SpringBootTest(
        classes = JwtItApplication.class,
        properties = {
                "security.verification-type=basic",
                "security.jwt.enabled=true",
                "security.jwt.secret=0123456789-0123456789-0123456789-secret",
                "security.allow-uris=/public/**",
                "security.login-uri="
        })
@AutoConfigureMockMvc
class JwtSecurityIntegrationTest {

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
                .andExpect(status().isUnauthorized());
    }
}
