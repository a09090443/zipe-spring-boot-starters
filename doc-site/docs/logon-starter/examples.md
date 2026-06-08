---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁提供 `logon-spring-boot-starter` 各功能的完整實作範例，涵蓋取得當前使用者、自訂驗證、稽核日誌、Handler 覆寫等常見情境。

## 基礎使用範例

### 範例一：在 Controller 取得當前登入帳號

`UserInfoUtil.loginUserId()` 是靜態方法，可在應用程式任何地方呼叫，從 `SecurityContextHolder` 取出目前執行緒對應的登入帳號。

```java
import com.zipe.util.UserInfoUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

    @GetMapping("/me")
    public String currentUserId() {
        String userId = UserInfoUtil.loginUserId();
        // 未登入時 userId 為字串 "anonymousUser"，需自行判斷
        if ("anonymousUser".equals(userId)) {
            return "尚未登入";
        }
        return "目前登入帳號：" + userId;
    }
}
```

### 範例二：在 Service 取得完整 SysUserVO

繼承 `SecurityBaseService` 後可呼叫 `fetchLoginUser()`，從 `HttpSession` 取出儲存的 `SysUserVO`。

```java
import com.zipe.base.service.SecurityBaseService;
import com.zipe.vo.SysUserVO;
import org.springframework.stereotype.Service;

@Service
public class DashboardService extends SecurityBaseService {

    public String getWelcomeMessage() {
        SysUserVO user = fetchLoginUser();  // 未登入或 Session 無資料時回傳 null
        if (user == null) {
            return "尚未登入";
        }
        return "歡迎，" + user.getUserId()
            + "（登入時間：" + user.getLoginTime() + "）";
    }
}
```

:::note SysUserVO 需由業務端寫入 Session
`SysUserVO` 以帳號 ID 為 key 儲存於 `HttpSession`，Starter 本身不自動填充。業務端通常在 `CustomLogonLogRecord.recordLoginSuccessLog()` 中建立 VO 並寫入 Session：

```java
@Override
public void recordLoginSuccessLog(String userId) {
    SysUserVO vo = new SysUserVO();
    vo.setUserId(userId);
    vo.setLoginTime(LocalDateTime.now().toString());
    httpSession.setAttribute(userId, vo);  // key 為帳號 ID
}
```
:::

### 範例三：限制登入者才能存取的端點

引入 Starter 後，未列於 `security.allow-uris` 的端點預設需要登入才能存取。Controller 本身不需要額外設定：

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecuredController {

    @GetMapping("/dashboard")
    public String dashboard() {
        // 此端點自動受 Spring Security 保護，未登入者會被導向登入頁
        return "僅登入使用者可見的頁面";
    }
}
```

---

## 進階使用範例

### 範例四：CUSTOM 模式——業務資料庫帳號驗證

這是最常見的擴充場景。繼承 `CommonLoginProcess` 自動獲得 ADMIN 動態密碼機制，只需實作 `verifyNormalUser()` 提供資料庫查詢邏輯。

```java
// 業務專案：config/DbAuthProvider.java
import com.zipe.service.CommonLoginProcess;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component("dbAuthProvider")  // Bean 名稱需與 security.custom-bean-name 一致
public class DbAuthProvider extends CommonLoginProcess {

    private final UserRepository userRepository;

    public DbAuthProvider(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        super(passwordEncoder);
        this.userRepository = userRepository;
    }

    @Override
    protected UsernamePasswordAuthenticationToken verifyNormalUser(String loginId, String password) {
        // 1. 查詢使用者
        UserEntity user = userRepository.findByUsername(loginId)
            .orElseThrow(() -> new UsernameNotFoundException("找不到使用者：" + loginId));

        // 2. 比對密碼（資料庫儲存的應為 BCrypt hash）
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("帳號或密碼錯誤");
        }

        // 3. 若需檢查帳號啟用狀態，可拋出 DisabledException
        // if (!user.isEnabled()) {
        //     throw new DisabledException("帳號尚未啟用");
        // }

        // 4. 回傳已認證的 Token：authorities 須為非 null（使 token 成為已認證狀態），
        //    且不要保留明文密碼（credentials 傳 null），避免敏感資訊殘留於安全內容
        return new UsernamePasswordAuthenticationToken(loginId, null, Collections.emptyList());
    }
}
```

:::tip 認證 Token 最佳實踐
回傳的 `UsernamePasswordAuthenticationToken` 應傳入**非 null** 的權限集合（如 `Collections.emptyList()` 或實際角色），此建構子會將 token 標記為已認證；同時將 credentials 設為 `null` 以免在 SecurityContext 中保留明文密碼。本模組的 `LdapUserDetailsService` 亦採此作法。
:::

**application.yml（業務專案）：**

```yaml
security:
  verification-type: custom
  custom-bean-name: dbAuthProvider
  login-uri: /login
  login-success-uri: /dashboard
  login-failure-uri: /login
  allow-uris: /static/**,/public/**
```

---

### 範例五：實作登入稽核日誌（CustomLogonLogRecord）

實作 `CustomLogonLogRecord` 介面並宣告為 Spring Bean，三個 Handler 會在對應事件發生時自動回呼。注意目前 `LogoutSuccessHandler` 存在設計缺陷，登出事件實際觸發的是 `recordFailureLog()` 而非 `recordLogoutSuccessLog()`。

```java
// 業務專案：service/AuditLogRecord.java
import com.zipe.service.CustomLogonLogRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("auditLogRecord")  // Bean 名稱需與 security.custom-record-log-bean 一致
public class AuditLogRecord implements CustomLogonLogRecord {

    private final AuditLogRepository repository;

    public AuditLogRecord(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 登入成功時由 LoginSuccessHandler 回呼。
     */
    @Override
    public void recordLoginSuccessLog(String userId) {
        repository.save(new AuditLogEntity(userId, "LOGIN_SUCCESS", LocalDateTime.now()));
    }

    /**
     * 登入失敗時由 LoginFailureHandler 回呼。
     * 注意：目前 LogoutSuccessHandler 的設計缺陷也會在登出時呼叫此方法，
     * 因此此方法可能同時代表「登入失敗」與「登出」兩個事件。
     */
    @Override
    public void recordFailureLog(String userId) {
        repository.save(new AuditLogEntity(userId, "LOGIN_FAILURE_OR_LOGOUT", LocalDateTime.now()));
    }

    /**
     * 此方法目前未被 LogoutSuccessHandler 呼叫（已知 Bug）。
     * 保留此實作以備 Starter 修復後立即生效。
     */
    @Override
    public void recordLogoutSuccessLog(String userId) {
        repository.save(new AuditLogEntity(userId, "LOGOUT_SUCCESS", LocalDateTime.now()));
    }
}
```

**application.yml：**

```yaml
security:
  record-log-enable: true
  custom-record-log-bean: auditLogRecord
```

---

### 範例六：覆寫 LoginSuccessHandler 回傳 JSON（前後端分離）

因模組 `application.yml` 啟用 `spring.main.allow-bean-definition-overriding: true`，業務專案宣告同名 Bean 即可覆寫預設 Handler 行為。

```java
// 業務專案：config/SecurityBeanConfig.java
import com.zipe.handler.LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;

import java.io.IOException;

@Configuration
public class SecurityBeanConfig {

    /**
     * 覆寫 Starter 預設的 LoginSuccessHandler。
     * 登入成功後回傳 JSON 而非頁面導向，適用於前後端分離架構。
     */
    @Bean
    public LoginSuccessHandler loginSuccessHandler() {
        return new LoginSuccessHandler(null, null) {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"status\":\"ok\",\"userId\":\"" + authentication.getName() + "\"}"
                );
            }
        };
    }
}
```

---

### 範例七：LDAP 模式設定

若組織已建置 Active Directory，只需在 YAML 中填入連線資訊，無需撰寫任何 Java 程式碼：

```yaml
security:
  enable: true
  verification-type: ldap
  login-uri: /login
  login-success-uri: /dashboard
  login-failure-uri: /login
  allow-uris: /static/**,/public/**,/resources/**
  csrf-enabled: false
  ldap:
    ip: 192.168.1.100              # AD 伺服器 IP
    domain: corp.example.com        # 網域，帳號 john 自動補全為 john@corp.example.com
    port: 389                       # LDAP 埠號（LDAPS 使用 636）
    dn: DC=corp,DC=example,DC=com   # 搜尋起始 DN
```

LDAP 驗證流程中，`LdapUserDetailsService` 會以 `sAMAccountName` 搜尋使用者並驗證密碼。驗證成功後回傳帳號（不含網域部分）作為 `Authentication.getName()`。

---

## 常見情境

### 情境一：讓部分路徑免登入

在 `security.allow-uris` 加入需要放行的路徑，支援 Ant 樣式：

```yaml
security:
  allow-uris: /static/**,/public/**,/api/health,/actuator/health,/error
```

### 情境二：限制特定角色才能存取

搭配 Spring Security 的方法層級授權（需確保 `Authentication` 有正確的 `GrantedAuthority`）：

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public String adminOnly() {
        return "管理者專屬頁面";
    }
}
```

:::note 目前 AuthenticationProvider 回傳空 authorities
`CommonLoginProcess` 及 `LdapUserDetailsService` 回傳的 `UsernamePasswordAuthenticationToken` 第三個參數（authorities）為 `null`，代表沒有任何角色。若需使用 `@PreAuthorize` 的角色控制，業務端的 `verifyNormalUser()` 需自行填入 `GrantedAuthority` 清單。
:::

### 情境三：開發環境停用所有驗證

```yaml
security:
  enable: false  # 全路徑免驗證，上線前務必移除或改為 true
```

---

## 常見問題

### 登入頁無限重新導向

**原因：** 登入頁本身未列入 `security.allow-uris`，導致存取登入頁也需要先登入，形成無限迴圈。

**解法：** 確認 `allow-uris` 包含登入相關路徑：

```yaml
security:
  login-uri: /login
  allow-uris: /static/**,/public/**
  # 注意：/login 本身由 Spring Security 自動放行，無需手動加入 allow-uris
```

### LDAP 連線失敗（LdapException）

**原因：** `security.ldap.ip`、`security.ldap.port` 設定錯誤，或網路不通。

**排查步驟：**
1. 確認 `security.ldap.ip` 與 `security.ldap.port` 正確
2. 測試網路連通：`telnet 192.168.1.100 389`
3. 確認 `security.ldap.dn` 格式正確（如 `DC=corp,DC=example,DC=com`）

### 登入失敗訊息設計建議

登入失敗回應應保持語義模糊（如「帳號或密碼錯誤」），不應分別提示「帳號不存在」或「密碼錯誤」，以避免洩漏帳號是否存在的資訊：

```java
// 推薦：統一回應，不透露帳號是否存在
throw new BadCredentialsException("帳號或密碼錯誤");

// 不推薦：洩漏帳號存在與否
throw new UsernameNotFoundException("找不到帳號：" + loginId);
```

### CUSTOM 模式啟動失敗（NullPointerException）

**原因：** `verification-type: custom` 但未設定 `custom-bean-name`。

**解法：** 確保兩個屬性同時設定：

```yaml
security:
  verification-type: custom
  custom-bean-name: dbAuthProvider  # 對應業務 Bean 的 @Component 名稱
```

:::tip 最佳實踐
- 生產環境請使用 `custom` 或 `ldap` 模式，不要使用 `basic`（hardcoded stub）
- 登入稽核日誌應寫入資料庫，並記錄時間戳記與來源 IP（IP 可從 `LoginSuccessHandler` 的回呼中取得，或在 `CustomLogonLogRecord` 中透過 `HttpServletRequest` 自行解析）
- 對連續登入失敗進行鎖定，防止暴力破解
:::
