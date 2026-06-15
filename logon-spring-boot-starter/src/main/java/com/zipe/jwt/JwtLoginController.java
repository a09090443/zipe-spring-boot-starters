package com.zipe.jwt;

import com.zipe.jwt.vo.JwtLoginRequest;
import com.zipe.jwt.vo.JwtLoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 內建 JWT 登入端點。委派 AuthenticationManager 驗帳密，成功後簽發 token。
 * 路徑由 {@code security.jwt.login-uri} 設定，預設 /api/login。
 * 以 {@code @ConditionalOnMissingBean} 在 SecurityConfiguration 註冊，業務專案可覆寫。
 */
@RestController
public class JwtLoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public JwtLoginController(AuthenticationManager authenticationManager,
                             JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("${security.jwt.login-uri:/api/login}")
    public ResponseEntity<JwtLoginResponse> login(@RequestBody JwtLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        String token = tokenProvider.generateToken(authentication.getName());
        return ResponseEntity.ok(new JwtLoginResponse(token, "Bearer"));
    }
}
