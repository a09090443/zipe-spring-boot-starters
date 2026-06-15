package com.zipe.jwt.it;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWT 整合測試專用啟動類別。
 *
 * <p>置於獨立套件 {@code com.zipe.jwt.it}，使 component scan 僅涵蓋本套件，
 * 不誤掃主程式碼的 {@code com.zipe.jwt} 條件式 Bean；{@code SecurityConfiguration}
 * 仍透過 auto-configuration 載入。</p>
 */
@SpringBootApplication
public class JwtItApplication {

    /** 受保護的測試端點，回傳當前登入者名稱。 */
    @RestController
    static class WhoAmIController {
        @GetMapping("/whoami")
        public String whoami(Authentication authentication) {
            return authentication.getName();
        }
    }
}
