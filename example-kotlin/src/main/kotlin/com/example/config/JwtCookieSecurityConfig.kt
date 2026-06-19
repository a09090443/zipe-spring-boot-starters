package com.example.config

import com.zipe.config.SecurityPropertyConfig
import com.zipe.jwt.JwtProperties
import com.zipe.jwt.JwtTokenProvider
import com.zipe.service.BasicUserServiceImpl
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * 「表單登入 + JWT cookie」混合模式示範（可切換）。
 *
 * logon-starter 內建的 JWT 模式（`security.jwt.enabled=true`）是**純無狀態 API**：
 * 停用表單登入頁、未帶 token 一律回 401，登入改走 `POST /api/login` 取 token。若希望保留
 * **瀏覽器表單登入頁**、又以 **JWT cookie** 取代 server session，即為本設定示範的混合模式。
 *
 * 運作方式：
 * 1. 沿用 Spring Security 內建登入頁（`GET /login`）與表單登入；驗帳密由
 *    [BasicUserServiceImpl]（讀 `security.basic.users`）+ [PasswordEncoder] 處理。
 * 2. 登入成功後，[jwtCookieSuccessHandler] 以 [JwtTokenProvider] 簽發 token、
 *    寫入 HttpOnly cookie，再導向 `/jsp`。
 * 3. 後續請求由 [JwtCookieAuthenticationFilter] 從 cookie 讀 token、驗證後設定
 *    SecurityContext；Session 為 [SessionCreationPolicy.STATELESS]，不依賴 server session。
 * 4. 登出時清除該 cookie。
 *
 * 以 `example.hybrid-jwt.enabled=true` 啟用；預設關閉，不影響範例既有的 basic 表單登入。
 *
 * **注意：** 啟用本設定時，請保持 `security.jwt.enabled=false`——否則 logon 內建的
 * `jwtSecurityFilterChain` 也會註冊，與本 chain 並存而衝突。
 *
 * @author Gary Tsai
 */
@Configuration
@ConditionalOnProperty(name = ["example.hybrid-jwt.enabled"], havingValue = "true")
class JwtCookieSecurityConfig {

    /**
     * 提供 [JwtTokenProvider]。`security.jwt.enabled=false` 時 logon 不會建立此 Bean，
     * 故在此補上；`@ConditionalOnMissingBean` 確保與 logon 內建版（若啟用）不衝突。
     *
     * @param jwtProperties JWT 設定屬性（logon 以 `@EnableConfigurationProperties` 註冊，恆存在）
     * @return JWT 簽發／驗證核心
     */
    @Bean
    @ConditionalOnMissingBean
    fun jwtTokenProvider(jwtProperties: JwtProperties): JwtTokenProvider = JwtTokenProvider(jwtProperties)

    /**
     * 混合模式的 [SecurityFilterChain]：表單登入頁 + JWT cookie 驗證（STATELESS）。
     *
     * @param http               HttpSecurity 設定建構器
     * @param basicUserService   使用者服務（讀 security.basic.users）
     * @param passwordEncoder    密碼編碼器（logon 提供）
     * @param tokenProvider      JWT 簽發／驗證核心
     * @param jwtProperties      JWT 設定屬性
     * @param securityConfig     Security 設定屬性（讀 allow-uris）
     * @return 混合模式的 SecurityFilterChain
     * @throws Exception 設定過程發生例外時拋出
     */
    @Bean
    fun hybridJwtFilterChain(
        http: HttpSecurity,
        basicUserService: BasicUserServiceImpl,
        passwordEncoder: PasswordEncoder,
        tokenProvider: JwtTokenProvider,
        jwtProperties: JwtProperties,
        securityConfig: SecurityPropertyConfig,
    ): SecurityFilterChain {
        val provider = DaoAuthenticationProvider(basicUserService)
        provider.setPasswordEncoder(passwordEncoder)

        val cookieFilter = JwtCookieAuthenticationFilter(tokenProvider, basicUserService, COOKIE_NAME)

        http.authenticationManager(ProviderManager(provider))
            .authorizeHttpRequests { auth ->
                auth
                    .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                    .requestMatchers(*allowUris(securityConfig)).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form.permitAll()
                    .successHandler(jwtCookieSuccessHandler(tokenProvider, jwtProperties))
            }
            .logout { logout ->
                logout.permitAll()
                    .addLogoutHandler { request, response, _ -> clearCookie(request, response) }
            }
            .csrf(AbstractHttpConfigurer<*, *>::disable)
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(cookieFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    /**
     * 登入成功處理器：簽發 JWT、寫入 HttpOnly cookie，再導向 `/jsp`。
     */
    private fun jwtCookieSuccessHandler(
        tokenProvider: JwtTokenProvider,
        jwtProperties: JwtProperties,
    ): AuthenticationSuccessHandler =
        AuthenticationSuccessHandler { request, response, authentication ->
            val token = tokenProvider.generateToken(authentication.name)
            val cookie = Cookie(COOKIE_NAME, token)
            cookie.isHttpOnly = true
            cookie.path = cookiePath(request)
            cookie.maxAge = jwtProperties.expirationSeconds.toInt()
            response.addCookie(cookie)
            response.sendRedirect(request.contextPath + "/jsp")
        }

    /**
     * 清除攜帶 token 的 cookie（登出用）。
     */
    private fun clearCookie(request: HttpServletRequest, response: HttpServletResponse) {
        val cookie = Cookie(COOKIE_NAME, "")
        cookie.isHttpOnly = true
        cookie.path = cookiePath(request)
        cookie.maxAge = 0
        response.addCookie(cookie)
    }

    /** cookie 的 path 取 context-path，根 context 時用 `/`。 */
    private fun cookiePath(request: HttpServletRequest): String {
        val contextPath = request.contextPath
        return if (contextPath.isNullOrEmpty()) "/" else contextPath
    }

    /** 解析 `security.allow-uris`（逗號分隔）為陣列，未設定時回傳空陣列。 */
    private fun allowUris(securityConfig: SecurityPropertyConfig): Array<String> {
        val allowUris = securityConfig.allowUris
        return if (allowUris.isNullOrBlank()) emptyArray() else allowUris.split(",").toTypedArray()
    }

    companion object {
        /** 攜帶 JWT 的 cookie 名稱。 */
        private const val COOKIE_NAME = "JWT_TOKEN"
    }
}
