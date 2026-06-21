package com.zipe.formlogin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zipe.formlogin.it.FormLoginItApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 預設 BASIC 表單登入模式的端到端回歸測試（<b>不含 iam-starter</b>）。
 *
 * <p>守護兩個曾使登入失效的串接缺陷已修復：</p>
 * <ol>
 *   <li>表單登入處理路徑 {@code POST /login} 不再被 {@code logout-uri}（預設 {@code /logout}）
 *       攔截——故 {@code admin/admin} 能真正完成認證（若 logout 仍綁 {@code /login}，
 *       此請求會被當成登出而無法認證）。</li>
 *   <li>HTTP Basic 採用本模組選定的 provider（而非多 provider Bean 時退讓的全域預設
 *       {@code DaoAuthenticationProvider}），故 {@code admin/admin} 可通過 Basic 驗證。</li>
 *   <li>登入失敗採 <b>redirect</b>（而非 forward）：避免 {@code login-failure-uri} 與
 *       {@code POST /login} 處理路徑相同時，forward 在 Spring Security 6/7「過濾所有
 *       dispatcher type」下無限轉發回認證 filter，造成 {@link StackOverflowError}。
 *       故失敗請求應回 3xx redirect 而非 forward。</li>
 * </ol>
 */
@SpringBootTest(
        classes = FormLoginItApplication.class,
        properties = {
                "security.verification-type=basic",
                "security.allow-uris=/public/**",
                "security.login-uri="
        })
@AutoConfigureMockMvc
class BasicFormLoginIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void formLogin_withValidAdmin_authenticatesAndRedirectsToSuccessUri() throws Exception {
        // 認證成功並導向 login-success-uri；若 POST /login 仍被當登出，admin/admin 不會通過認證
        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "admin")
                        .with(csrf()))
                .andExpect(authenticated().withUsername("admin"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void formLogin_withWrongPassword_isUnauthenticatedAndRedirects() throws Exception {
        // 失敗須以 redirect（3xx）導向 login-failure-uri，而非 forward；
        // 若改回 setUseForward(true)，此處會是 forward 而非 3xx，藉此守護迴圈缺陷不再復發
        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "wrong-password")
                        .with(csrf()))
                .andExpect(unauthenticated())
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void httpBasic_withValidAdmin_accessesProtectedResource() throws Exception {
        mockMvc.perform(get("/whoami").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    void protectedResource_withoutAuthentication_isRejected() throws Exception {
        // 未認證存取受保護資源：安全內容無已認證主體（依 entry point 可能回 401 或導向登入頁）
        mockMvc.perform(get("/whoami"))
                .andExpect(unauthenticated());
    }

    @Test
    void logout_atDefaultLogoutUri_isHandled() throws Exception {
        // /logout 由 LogoutFilter 處理並導向；證明登出已搬離 /login，與表單登入路徑分離
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
