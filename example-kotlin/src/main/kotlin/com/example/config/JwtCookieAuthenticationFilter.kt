package com.example.config

import com.zipe.jwt.JwtTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 「表單登入 + JWT cookie」混合模式用的驗證過濾器。
 *
 * logon-starter 內建的 `JwtAuthenticationFilter` 只從 `Authorization` 標頭讀 token；
 * 本過濾器改從 **cookie** 讀取 JWT，使瀏覽器在表單登入後即可僅憑 cookie 帶 token 存取受保護頁面
 * （無 server session）。驗證成功後將 [UsernamePasswordAuthenticationToken] 寫入
 * [SecurityContextHolder]。
 *
 * 僅在 `example.hybrid-jwt.enabled=true` 時由 `JwtCookieSecurityConfig` 裝配。
 *
 * @author Gary Tsai
 *
 * @param tokenProvider      JWT 簽發／驗證核心
 * @param userDetailsService 依 username 載入權限的使用者服務
 * @param cookieName         攜帶 token 的 cookie 名稱
 */
class JwtCookieAuthenticationFilter(
    /** JWT 簽發／驗證核心（重用 logon-starter 的實作）。 */
    private val tokenProvider: JwtTokenProvider,
    /** 依 username 載入權限的使用者服務（範例為讀 security.basic.users 的 BasicUserServiceImpl）。 */
    private val userDetailsService: UserDetailsService,
    /** 攜帶 token 的 cookie 名稱。 */
    private val cookieName: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)
        if (StringUtils.isNotBlank(token) &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            try {
                val username = tokenProvider.validateAndGetUsername(token)
                val user = userDetailsService.loadUserByUsername(username)
                val auth = UsernamePasswordAuthenticationToken(user, null, user.authorities)
                auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = auth
            } catch (ex: Exception) {
                // token 無效 / 過期 / 使用者不存在：不寫入 context，交由後續授權規則處理
                log.debug("JWT cookie 驗證失敗: {}", ex.message)
            }
        }
        filterChain.doFilter(request, response)
    }

    /**
     * 從請求 cookie 中取出 token 字串，無對應 cookie 時回傳 `null`。
     */
    private fun resolveToken(request: HttpServletRequest): String? {
        val cookies = request.cookies ?: return null
        return cookies.firstOrNull { cookieName == it.name }?.value
    }

    companion object {
        private val log = LoggerFactory.getLogger(JwtCookieAuthenticationFilter::class.java)
    }
}
