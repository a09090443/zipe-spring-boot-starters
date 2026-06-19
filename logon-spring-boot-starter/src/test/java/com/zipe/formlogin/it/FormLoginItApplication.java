package com.zipe.formlogin.it;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 預設（BASIC / 表單登入）模式整合測試專用啟動類別。
 *
 * <p>置於獨立套件 {@code com.zipe.formlogin.it}，使 component scan 僅涵蓋本套件，
 * 不誤掃其他測試的 Bean；{@code SecurityConfiguration} 仍透過 auto-configuration 載入。
 * 本測試情境<b>不含 iam-starter</b>，用以驗證移除 iam 依賴後，logon 的表單登入與
 * HTTP Basic 仍正常運作。</p>
 */
@SpringBootApplication
public class FormLoginItApplication {

    /** 受保護的測試端點，回傳當前登入者名稱。 */
    @RestController
    static class WhoAmIController {
        @GetMapping("/whoami")
        public String whoami(Authentication authentication) {
            return authentication.getName();
        }
    }
}
