package com.example.config;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.jwt.JwtProperties;
import com.zipe.jwt.JwtTokenProvider;
import com.zipe.service.BasicUserServiceImpl;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 「表單登入 + JWT cookie」混合模式示範（可切換）。
 *
 * <p>logon-starter 內建的 JWT 模式（{@code security.jwt.enabled=true}）是<b>純無狀態 API</b>：
 * 停用表單登入頁、未帶 token 一律回 401，登入改走 {@code POST /api/login} 取 token。若希望保留
 * <b>瀏覽器表單登入頁</b>、又以 <b>JWT cookie</b> 取代 server session，即為本設定示範的混合模式。</p>
 *
 * <p>運作方式：</p>
 * <ol>
 *   <li>沿用 Spring Security 內建登入頁（{@code GET /login}）與表單登入；驗帳密由
 *       {@link BasicUserServiceImpl}（讀 {@code security.basic.users}）+ {@link PasswordEncoder} 處理。</li>
 *   <li>登入成功後，{@link #jwtCookieSuccessHandler} 以 {@link JwtTokenProvider} 簽發 token、
 *       寫入 HttpOnly cookie，再導向 {@code /jsp}。</li>
 *   <li>後續請求由 {@link JwtCookieAuthenticationFilter} 從 cookie 讀 token、驗證後設定
 *       SecurityContext；Session 為 {@link SessionCreationPolicy#STATELESS}，不依賴 server session。</li>
 *   <li>登出時清除該 cookie。</li>
 * </ol>
 *
 * <p>以 {@code example.hybrid-jwt.enabled=true} 啟用；預設關閉，不影響範例既有的 basic 表單登入。</p>
 *
 * <p><b>注意：</b>啟用本設定時，請保持 {@code security.jwt.enabled=false}——否則 logon 內建的
 * {@code jwtSecurityFilterChain} 也會註冊，與本 chain 並存而衝突。</p>
 *
 * @author Gary Tsai
 */
@Configuration
@ConditionalOnProperty(name = "example.hybrid-jwt.enabled", havingValue = "true")
public class JwtCookieSecurityConfig {

    /** 攜帶 JWT 的 cookie 名稱。 */
    private static final String COOKIE_NAME = "JWT_TOKEN";

    /**
     * 提供 {@link JwtTokenProvider}。{@code security.jwt.enabled=false} 時 logon 不會建立此 Bean，
     * 故在此補上；{@code @ConditionalOnMissingBean} 確保與 logon 內建版（若啟用）不衝突。
     *
     * @param jwtProperties JWT 設定屬性（logon 以 {@code @EnableConfigurationProperties} 註冊，恆存在）
     * @return JWT 簽發／驗證核心
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(jwtProperties);
    }

    /**
     * 混合模式的 {@link SecurityFilterChain}：表單登入頁 + JWT cookie 驗證（STATELESS）。
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
    public SecurityFilterChain hybridJwtFilterChain(HttpSecurity http,
                                                    BasicUserServiceImpl basicUserService,
                                                    PasswordEncoder passwordEncoder,
                                                    JwtTokenProvider tokenProvider,
                                                    JwtProperties jwtProperties,
                                                    SecurityPropertyConfig securityConfig) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(basicUserService);
        provider.setPasswordEncoder(passwordEncoder);

        JwtCookieAuthenticationFilter cookieFilter =
                new JwtCookieAuthenticationFilter(tokenProvider, basicUserService, COOKIE_NAME);

        http.authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers(allowUris(securityConfig)).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.permitAll()
                        .successHandler(jwtCookieSuccessHandler(tokenProvider, jwtProperties)))
                .logout(logout -> logout.permitAll()
                        .addLogoutHandler((request, response, authentication) -> clearCookie(request, response)))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(cookieFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 登入成功處理器：簽發 JWT、寫入 HttpOnly cookie，再導向 {@code /jsp}。
     */
    private AuthenticationSuccessHandler jwtCookieSuccessHandler(JwtTokenProvider tokenProvider,
                                                                JwtProperties jwtProperties) {
        return (request, response, authentication) -> {
            String token = tokenProvider.generateToken(authentication.getName());
            Cookie cookie = new Cookie(COOKIE_NAME, token);
            cookie.setHttpOnly(true);
            cookie.setPath(cookiePath(request));
            cookie.setMaxAge((int) jwtProperties.getExpirationSeconds());
            response.addCookie(cookie);
            response.sendRedirect(request.getContextPath() + "/jsp");
        };
    }

    /**
     * 清除攜帶 token 的 cookie（登出用）。
     */
    private void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath(cookiePath(request));
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /** cookie 的 path 取 context-path，根 context 時用 {@code /}。 */
    private String cookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return contextPath == null || contextPath.isEmpty() ? "/" : contextPath;
    }

    /** 解析 {@code security.allow-uris}（逗號分隔）為陣列，未設定時回傳空陣列。 */
    private String[] allowUris(SecurityPropertyConfig securityConfig) {
        String allowUris = securityConfig.getAllowUris();
        return allowUris == null || allowUris.isBlank() ? new String[0] : allowUris.split(",");
    }
}
