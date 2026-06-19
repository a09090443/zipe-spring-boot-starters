package com.example.config;

import com.zipe.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 「表單登入 + JWT cookie」混合模式用的驗證過濾器。
 *
 * <p>logon-starter 內建的 {@code JwtAuthenticationFilter} 只從 {@code Authorization} 標頭讀 token；
 * 本過濾器改從 <b>cookie</b> 讀取 JWT，使瀏覽器在表單登入後即可僅憑 cookie 帶 token 存取受保護頁面
 * （無 server session）。驗證成功後將 {@link UsernamePasswordAuthenticationToken} 寫入
 * {@link SecurityContextHolder}。</p>
 *
 * <p>僅在 {@code example.hybrid-jwt.enabled=true} 時由 {@code JwtCookieSecurityConfig} 裝配。</p>
 *
 * @author Gary Tsai
 */
@Slf4j
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 簽發／驗證核心（重用 logon-starter 的實作）。 */
    private final JwtTokenProvider tokenProvider;

    /** 依 username 載入權限的使用者服務（範例為讀 security.basic.users 的 BasicUserServiceImpl）。 */
    private final UserDetailsService userDetailsService;

    /** 攜帶 token 的 cookie 名稱。 */
    private final String cookieName;

    /**
     * 建構過濾器。
     *
     * @param tokenProvider      JWT 簽發／驗證核心
     * @param userDetailsService 依 username 載入權限的使用者服務
     * @param cookieName         攜帶 token 的 cookie 名稱
     */
    public JwtCookieAuthenticationFilter(JwtTokenProvider tokenProvider,
                                         UserDetailsService userDetailsService,
                                         String cookieName) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.isNotBlank(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = tokenProvider.validateAndGetUsername(token);
                UserDetails user = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                // token 無效 / 過期 / 使用者不存在：不寫入 context，交由後續授權規則處理
                log.debug("JWT cookie 驗證失敗: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 從請求 cookie 中取出 token 字串，無對應 cookie 時回傳 {@code null}。
     */
    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
