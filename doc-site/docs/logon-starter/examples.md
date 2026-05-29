---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁示範 `logon-spring-boot-starter` 的登入日誌記錄、當前使用者取得與自訂驗證等實作。

## 基礎使用範例

### 取得當前登入使用者

```java
import com.zipe.util.UserInfoUtil;
import com.zipe.vo.SysUserVO;

public class CurrentUserExample {

    public String currentUserName() {
        SysUserVO user = UserInfoUtil.getCurrentUser();
        return user != null ? user.getUserName() : "anonymous";
    }
}
```

### 在 Controller 中限制存取

模組接管 Security 設定後，未列於 `permit-all` 的端點皆需登入才能存取：

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecuredController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "僅登入使用者可見";
    }
}
```

## 進階使用範例

### 實作自訂登入日誌記錄

實作 `CustomLogonLogRecord` 介面並註冊為 Bean，登入成功／失敗時模組會自動回呼：

```java
import com.zipe.service.CustomLogonLogRecord;
import org.springframework.stereotype.Component;

@Component
public class DbLogonLogRecord implements CustomLogonLogRecord {

    private final LogonLogRepository repository;

    public DbLogonLogRecord(LogonLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordSuccess(String username, String ip) {
        repository.save(new LogonLog(username, ip, "SUCCESS"));
    }

    @Override
    public void recordFailure(String username, String ip, String reason) {
        repository.save(new LogonLog(username, ip, "FAIL: " + reason));
    }
}
```

### 自訂登入成功後的行為

可參考 `LoginSuccessHandler` 的設計，於登入成功後回傳 JSON 而非頁面導向，適用於前後端分離架構：

```java
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class JsonLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(jakarta.servlet.http.HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws java.io.IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"ok\"}");
    }
}
```

## 常見情境

### 情境一：DB 與 LDAP 混合驗證

先嘗試 LDAP，失敗時退回資料庫驗證，可透過實作自訂 `AuthenticationProvider` 串接兩種來源達成。

### 情境二：限制特定角色存取

搭配 Spring Security 的方法層級授權：

```java
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<SysUserVO> listUsers() {
    return userService.findAll();
}
```

### 情境三：記錄登入來源 IP 與時間

如「實作自訂登入日誌記錄」範例所示，於 `recordSuccess` / `recordFailure` 中寫入稽核資料表，即可保留完整登入軌跡。

## 常見問題

- **登入頁無限重新導向**：通常是登入頁本身未列入 `permit-all`，導致存取登入頁也需登入而形成迴圈。
- **LDAP 連線失敗**：檢查 `zipe.ldap.url`、`base-dn` 與管理者帳密，並確認網路可連通 LDAP 伺服器。
- **密碼比對永遠失敗**：確認資料庫密碼為 `PasswordEncoder` 編碼後的值，且應用使用相同的編碼器。

:::tip 最佳實踐
登入失敗訊息應保持模糊（如「帳號或密碼錯誤」），避免分別提示帳號不存在或密碼錯誤而洩漏帳號是否存在的資訊；同時建議對連續失敗進行鎖定，以防暴力破解。
:::
