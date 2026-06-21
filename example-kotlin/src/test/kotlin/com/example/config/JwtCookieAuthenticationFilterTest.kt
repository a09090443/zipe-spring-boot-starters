package com.example.config

import com.zipe.jwt.JwtTokenProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService

/**
 * [JwtCookieAuthenticationFilter] 的單元測試（不需 Spring context／資料庫）。
 *
 * 驗證混合模式下「從 cookie 讀 JWT → 驗證 → 設定 SecurityContext」的核心行為。
 */
class JwtCookieAuthenticationFilterTest : FunSpec({

    val cookieName = "JWT_TOKEN"

    val tokenProvider = mock(JwtTokenProvider::class.java)
    val userDetailsService = mock(UserDetailsService::class.java)
    val filter = JwtCookieAuthenticationFilter(tokenProvider, userDetailsService, cookieName)

    // 每個測試後清除 SecurityContext，避免互相污染
    afterTest {
        SecurityContextHolder.clearContext()
    }

    test("帶有效 token 的 cookie：應載入使用者並把已認證的 token 寫入 SecurityContext") {
        val user: UserDetails = User.withUsername("user01").password("x")
            .authorities(listOf(SimpleGrantedAuthority("ORDER_EXPORT"))).build()
        `when`(tokenProvider.validateAndGetUsername("good-token")).thenReturn("user01")
        `when`(userDetailsService.loadUserByUsername("user01")).thenReturn(user)

        val request = MockHttpServletRequest()
        request.setCookies(Cookie(cookieName, "good-token"))

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        val auth = SecurityContextHolder.getContext().authentication
        auth.shouldNotBeNull()
        auth.name shouldBe "user01"
        auth.authorities.map { it.authority } shouldContainExactly listOf("ORDER_EXPORT")
    }

    test("無 cookie：不應建立任何認證") {
        val request = MockHttpServletRequest()

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        SecurityContextHolder.getContext().authentication shouldBe null
    }

    test("token 無效（驗證拋例外）：不應建立認證，且不應中斷請求鏈") {
        `when`(tokenProvider.validateAndGetUsername("bad")).thenThrow(RuntimeException("invalid"))

        val request = MockHttpServletRequest()
        request.setCookies(Cookie(cookieName, "bad"))

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        SecurityContextHolder.getContext().authentication shouldBe null
    }
})
