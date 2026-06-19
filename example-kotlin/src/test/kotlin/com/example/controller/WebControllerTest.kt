package com.example.controller

import com.example.base.TestBase
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Web 視圖端點整合測試：以 BASIC 模式（admin/admin）認證後分別存取 Thymeleaf 與 JSP 頁面，
 * 驗證皆回應 HTTP 200。
 */
@AutoConfigureMockMvc
class WebControllerTest(
    private val mockMvc: MockMvc,
) : TestBase({

    test("testThymeleaf") {
        mockMvc.perform(get("/thymeleaf").with(httpBasic("admin", "admin"))).andExpect(status().isOk)
    }

    test("testJsp") {
        mockMvc.perform(get("/jsp").with(httpBasic("admin", "admin"))).andExpect(status().isOk)
    }
})
