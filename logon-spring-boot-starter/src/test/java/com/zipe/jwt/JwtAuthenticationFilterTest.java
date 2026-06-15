package com.zipe.jwt;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private JwtTokenProvider provider() {
        JwtProperties p = new JwtProperties();
        p.setSecret("0123456789-0123456789-0123456789-secret");
        JwtTokenProvider provider = new JwtTokenProvider(p);
        provider.init();
        return provider;
    }

    @Test
    void validToken_shouldPopulateSecurityContext() throws Exception {
        JwtTokenProvider provider = provider();
        UserDetailsService uds = mock(UserDetailsService.class);
        UserDetails user = new User("alice", "x", AuthorityUtils.createAuthorityList("ROLE_USER"));
        when(uds.loadUserByUsername("alice")).thenReturn(user);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, uds, new JwtProperties());
        String token = provider.generateToken("alice");

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals("alice",
                SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void invalidToken_shouldNotPopulateContext() throws Exception {
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(provider(), mock(UserDetailsService.class), new JwtProperties());

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer not-a-real-token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
