package com.example.controller

import com.example.base.TestBase
import org.hamcrest.Matchers.containsString
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * REST 端點整合測試：以 BASIC 模式（user01/1234，帳密來自 security.basic.users）認證後
 * 存取受保護的 `/rest/sayHello`，驗證回應內容包含預期問候字串。
 */
@AutoConfigureMockMvc
class RestControllerTest(
    private val mockMvc: MockMvc,
) : TestBase({

    test("sayHelloTest") {
        // BASIC 模式以 user01/1234 認證後存取受保護端點（帳密來自 security.basic.users）
        mockMvc.perform(get("/rest/sayHello?name=John").with(httpBasic("user01", "1234")))
            .andDo(print()).andExpect(status().isOk)
            .andExpect(content().string(containsString("Hello,  John!")))
    }
})
