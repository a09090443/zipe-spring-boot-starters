package com.zipe.formlogin;

import com.zipe.formlogin.it.FormLoginItApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 驗證 {@code security.basic.users} 自訂帳號於 BASIC 模式的端到端行為（<b>不含 iam-starter</b>）。
 *
 * <p>守護：設定 users 後，①設定的帳號（明文密碼）可通過 HTTP Basic 驗證；
 * ②內建的 {@code admin/admin} fallback 被取代、不再可用。</p>
 */
@SpringBootTest(
        classes = FormLoginItApplication.class,
        properties = {
                "security.verification-type=basic",
                "security.allow-uris=/public/**",
                "security.login-uri=",
                "security.basic.users[0].username=user01",
                "security.basic.users[0].password=1234",
                "security.basic.users[0].authorities[0]=admin"
        })
@AutoConfigureMockMvc
class ConfiguredBasicUsersIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void httpBasic_withConfiguredUser_accessesProtectedResource() throws Exception {
        mockMvc.perform(get("/whoami").with(httpBasic("user01", "1234")))
                .andExpect(status().isOk())
                .andExpect(content().string("user01"));
    }

    @Test
    void httpBasic_withFallbackAdmin_isRejectedWhenUsersConfigured() throws Exception {
        // 一旦設定 users，admin/admin fallback 失效
        mockMvc.perform(get("/whoami").with(httpBasic("admin", "admin")))
                .andExpect(unauthenticated());
    }
}
