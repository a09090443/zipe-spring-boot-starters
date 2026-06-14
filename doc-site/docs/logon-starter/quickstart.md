---
id: quickstart
title: 快速開始
sidebar_position: 2
---

# 快速開始

本頁示範如何啟用表單登入，並在 Controller 中取得當前登入使用者。

## 前置需求

- JDK 17 以上、Spring Boot 4.0.x。
- 已將 `logon-spring-boot-starter` 安裝至本地 Maven Repository。
- 選定驗證模式：`basic`（開發測試）、`ldap`（企業 AD）或 `custom`（業務資料庫）。

:::note 不得自行定義 SecurityFilterChain
本模組會接管 Spring Security 的主要設定，業務專案**不得**另行定義 `SecurityFilterChain` Bean，以免 Spring Context 啟動失敗。
:::

## Step 1：安裝模組

```bash
cd logon-spring-boot-starter
./mvnw clean install -DskipTests
```

## Step 2：加入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>logon-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

## Step 3：設定 application.yml

以下範例使用 `basic` 模式（適合快速驗證功能，預設帳密為 `admin` / `admin`）：

```yaml
security:
  enable: true
  verification-type: basic       # basic | ldap | custom
  login-uri: /login              # 自訂登入頁路徑；若移除此設定則使用 Spring Security 預設登入頁
  login-success-uri: /dashboard  # 登入成功後的導向路徑
  login-failure-uri: /login      # 登入失敗後的轉送路徑（伺服器端 forward）
  allow-uris: /static/**,/public/**  # 免驗證放行的路徑（逗號分隔）
  csrf-enabled: false            # 是否啟用 CSRF 保護
```

:::warning BASIC 模式僅適合開發測試
`verification-type: basic` 使用的 `BasicUserServiceImpl` 為 hardcoded stub，帳號固定為 `admin`，密碼固定為 `admin`。**生產環境必須改用 `custom` 或 `ldap` 模式。**
:::

## Step 4：取得當前登入使用者

模組提供兩種方式取得當前登入者：

### 方式一：靜態工具 UserInfoUtil（取得帳號 ID）

```java
import com.zipe.util.UserInfoUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

    @GetMapping("/me")
    public String currentUserId() {
        String userId = UserInfoUtil.loginUserId();
        // 未登入時回傳字串 "anonymousUser"，呼叫端需自行判斷
        return "anonymousUser".equals(userId) ? "尚未登入" : userId;
    }
}
```

### 方式二：繼承 SecurityBaseService（取得完整 SysUserVO）

```java
import com.zipe.base.service.SecurityBaseService;
import com.zipe.vo.SysUserVO;
import org.springframework.stereotype.Service;

@Service
public class DashboardService extends SecurityBaseService {

    public String greeting() {
        SysUserVO user = fetchLoginUser();  // 未登入時回傳 null
        if (user == null) {
            return "尚未登入";
        }
        return "歡迎，" + user.getUserId() + "（登入時間：" + user.getLoginTime() + "）";
    }
}
```

:::note SysUserVO 的 loginTime 由業務端填入
`SysUserVO` 儲存於 `HttpSession`（以帳號 ID 為 key）。`loginTime` 欄位需要業務端在登入成功後自行寫入 Session，Starter 本身不自動填充。
:::

## Step 5：執行驗證

啟動應用程式：

```bash
cd starters_example   # 或您的業務專案目錄
./mvnw spring-boot:run
```

開啟瀏覽器，前往 `http://localhost:8080/login`，輸入帳號 `admin`、密碼 `admin`，登入成功後應被導向 `login-success-uri`（範例為 `/dashboard`）。

接著呼叫 `http://localhost:8080/me`，應回傳目前登入帳號 `admin`。

:::tip 放行靜態資源路徑
`allow-uris` 務必包含登入頁本身所需的靜態資源路徑（CSS / JS / 圖片等），否則未登入時無法正常載入登入頁樣式：

```yaml
security:
  allow-uris: /static/**,/public/**,/resources/**,/webjars/**
```
:::

:::tip security.enable=false 可關閉所有驗證
開發初期若需要全路徑放行，可暫時設定：

```yaml
security:
  enable: false
```

此設定會讓所有路徑免登入即可存取，**上線前務必移除或改回 `true`**。
:::

## LDAP 模式快速設定

若組織已建置 Active Directory / LDAP，改用以下設定：

```yaml
security:
  verification-type: ldap
  login-uri: /login
  login-success-uri: /dashboard
  login-failure-uri: /login
  allow-uris: /static/**,/public/**
  ldap:
    ip: 192.168.1.100        # LDAP / AD 伺服器 IP
    domain: corp.example.com  # 網域名稱（帳號自動補全為 userId@domain）
    port: 389                 # LDAP 埠號（LDAPS 通常為 636）
    dn: DC=corp,DC=example,DC=com  # 搜尋起始 DN
```

## CUSTOM 模式快速設定

業務帳號存於資料庫時，在業務專案中實作 `CommonLoginProcess`，再設定 Bean 名稱：

```yaml
security:
  verification-type: custom
  custom-bean-name: dbAuthProvider  # 對應業務專案中 @Component("dbAuthProvider") 的 Bean
  login-uri: /login
  login-success-uri: /dashboard
  login-failure-uri: /login
  allow-uris: /static/**,/public/**
```

詳細實作步驟請參閱[使用範例](./examples.md)。

:::warning CSRF 設定
Spring Security 預設啟用 CSRF 保護（`csrf-enabled: true`）。若使用傳統表單登入，請確保表單包含 CSRF token；若為純 REST API 或前後端分離架構，可設定 `csrf-enabled: false`。
:::
