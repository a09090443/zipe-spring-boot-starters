package com.example.config;

import com.zipe.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link JwtCookieAuthenticationFilter} 的單元測試（不需 Spring context／資料庫）。
 *
 * <p>驗證混合模式下「從 cookie 讀 JWT → 驗證 → 設定 SecurityContext」的核心行為。</p>
 */
class JwtCookieAuthenticationFilterTest {

    private static final String COOKIE = "JWT_TOKEN";

    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtCookieAuthenticationFilter filter =
            new JwtCookieAuthenticationFilter(tokenProvider, userDetailsService, COOKIE);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 帶有效 token 的 cookie：應載入使用者並把已認證的 token 寫入 SecurityContext。
     */
    @Test
    void validCookieTokenSetsAuthentication() throws Exception {
        UserDetails user = User.withUsername("user01").password("x")
                .authorities(List.of(new SimpleGrantedAuthority("ORDER_EXPORT"))).build();
        when(tokenProvider.validateAndGetUsername("good-token")).thenReturn("user01");
        when(userDetailsService.loadUserByUsername("user01")).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE, "good-token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("user01");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ORDER_EXPORT");
    }

    /**
     * 無 cookie：不應建立任何認證。
     */
    @Test
    void noCookieDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * token 無效（驗證拋例外）：不應建立認證，且不應中斷請求鏈。
     */
    @Test
    void invalidTokenDoesNotAuthenticate() throws Exception {
        when(tokenProvider.validateAndGetUsername("bad")).thenThrow(new RuntimeException("invalid"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE, "bad"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
