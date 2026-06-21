package com.zipe.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zipe.jwt.vo.JwtLoginRequest;
import com.zipe.jwt.vo.JwtLoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class JwtLoginControllerTest {

    private JwtTokenProvider provider() {
        JwtProperties p = new JwtProperties();
        p.setSecret("0123456789-0123456789-0123456789-secret");
        JwtTokenProvider provider = new JwtTokenProvider(p);
        provider.init();
        return provider;
    }

    @Test
    void login_validCredentials_returnsToken() {
        AuthenticationManager am = mock(AuthenticationManager.class);
        Authentication auth = new UsernamePasswordAuthenticationToken("alice", "pw");
        when(am.authenticate(any())).thenReturn(auth);

        JwtTokenProvider provider = provider();
        JwtLoginController controller = new JwtLoginController(am, provider);

        JwtLoginRequest req = new JwtLoginRequest();
        req.setUsername("alice");
        req.setPassword("pw");

        ResponseEntity<JwtLoginResponse> res = controller.login(req);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Bearer", res.getBody().getTokenType());
        assertEquals("alice", provider.validateAndGetUsername(res.getBody().getToken()));
    }

    @Test
    void login_badCredentials_propagatesException() {
        AuthenticationManager am = mock(AuthenticationManager.class);
        when(am.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        JwtLoginController controller = new JwtLoginController(am, provider());
        JwtLoginRequest req = new JwtLoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        assertThrows(BadCredentialsException.class, () -> controller.login(req));
    }
}
