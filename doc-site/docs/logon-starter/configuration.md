---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `logon-spring-boot-starter` 所有可設定屬性，分為 Security 主屬性（`security.*`）與 LDAP 子屬性（`security.ldap.*`）兩大類。

:::note 屬性 Prefix
所有屬性以 `security` 為根 prefix，**不含** `zipe`、`spring.security` 或其他前綴。
:::

## Security 主屬性（`security.*`）

| 屬性鍵 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `security.enable` | Boolean | `true` | 安全控制總開關；設為 `false` 時全部路徑免驗證放行（會輸出 WARN 日誌，請勿用於正式環境） |
| `security.verification-type` | String | `basic` | 憑證來源（怎麼驗帳密）：`basic` / `ldap` / `custom`（大小寫不敏感）。與 `security.jwt.enabled`（登入後狀態策略）正交 |
| `security.login-uri` | String | 無 | 自訂登入頁路徑；設定此值時採用 `customLoginConfigure`（STATELESS Session）；若留空則使用 Spring Security 預設登入頁（Stateful Session） |
| `security.login-success-uri` | String | `/` | 登入成功後的導向路徑 |
| `security.login-failure-uri` | String | `/error` | 登入失敗後的目標路徑（採用 redirect）。搭配預設登入頁請設 `/login`（**勿用 `/login?error`**，自訂 failureHandler 下產生器不認得、會 404）；本模組已固定用 redirect 避免 forward 迴圈，詳見下方說明 |
| `security.logout-uri` | String | `/logout` | 觸發登出的路徑。**切勿設為 `/login`**（與表單登入處理路徑相同），否則 LogoutFilter 會搶先攔截登入請求、使表單登入失效 |
| `security.allow-uris` | String | 無 | 免驗證放行的路徑，逗號分隔，支援 Ant 樣式，如 `/static/**,/public/**` |
| `security.csrf-enabled` | Boolean | `true` | CSRF 保護開關；傳統表單應維持 `true`，純 REST API 視情況可設為 `false` |
| `security.frame-options-mode` | Enum | `SAMEORIGIN` | X-Frame-Options（點擊劫持防護）模式：`SAMEORIGIN`（僅同源可內嵌）/ `DENY`（禁止內嵌）/ `DISABLE`（停用，不建議） |
| `security.record-log-enable` | Boolean | `false` | 是否啟用登入稽核回呼；設為 `true` 時必須同時設定 `custom-record-log-bean` |
| `security.custom-record-log-bean` | String | 無 | `record-log-enable=true` 時必填；業務端 `CustomLogonLogRecord` Bean 的名稱 |
| `security.custom-bean-name` | String | 無 | `verification-type=custom` 時必填；業務端 `AuthenticationProvider` Bean 的名稱 |

## BASIC 使用者子屬性（`security.basic.*`）

僅在 `security.verification-type: basic` 時生效。設定 `security.basic.users` 後，BASIC 模式的使用者來源改為此清單；**未設定時 fallback 回內建的 `admin/admin`**（開發測試用，向後相容）。對應 `BasicUserPropertyConfig` / `BasicUser`。

| 屬性鍵 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `security.basic.users` | List | 空清單 | 可登入的使用者清單；為空時 fallback 回內建 `admin/admin` |
| `security.basic.users[i].username` | String | 無 | 登入帳號 |
| `security.basic.users[i].password` | String | 無 | 登入密碼。支援**明文**（載入時自動以 `PasswordEncoder` 編碼）與**帶 `{id}` 前綴的預雜湊值**（如 `{bcrypt}$2a$...`，原值採用、依前綴比對） |
| `security.basic.users[i].authorities` | List&lt;String&gt; | 空清單 | 權限清單，每個字串直接轉為 `SimpleGrantedAuthority`，**不自動加 `ROLE_` 前綴**（要當角色請自行寫 `ROLE_xxx`） |

:::note 引入 iam-starter 時此設定不生效
引入 `iam-spring-boot-starter` 後，`IamUserDetailsService` 會接管 BASIC 模式的帳號查詢、改查 `iam_account` 資料表，`security.basic.users` 不再被使用。此設定適用於**未引入 iam、純 logon BASIC** 的情境。
:::

## JWT 子屬性（`security.jwt.*`）

設定 `security.jwt.enabled: true` 時生效，與 `verification-type` 正交（可疊加於 basic / ldap / custom 任一憑證來源）。對應 `JwtProperties`。

| 屬性鍵 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `security.jwt.enabled` | Boolean | `false` | 是否啟用 JWT 無狀態登入；`true` 時以 JWT filter chain 取代表單登入與 session，登入仍依 `verification-type` 驗帳密 |
| `security.jwt.algorithm` | String | `HS256` | 簽章演算法：`HS256`（對稱）或 `RS256`（非對稱） |
| `security.jwt.secret` | String | 無 | HS256 對稱密鑰，長度至少 32 位元組；`algorithm=HS256` 時必填（建議以環境變數注入） |
| `security.jwt.private-key-location` | String | 無 | RS256 私鑰位置（PEM，簽 token 用），支援 `classpath:` / `file:`；`algorithm=RS256` 時必填 |
| `security.jwt.public-key-location` | String | 無 | RS256 公鑰位置（PEM，驗 token 用），支援 `classpath:` / `file:`；`algorithm=RS256` 時必填 |
| `security.jwt.expiration-seconds` | long | `3600` | access token 有效秒數，預設 3600（1 小時） |
| `security.jwt.login-uri` | String | `/api/login` | 內建登入端點路徑 |
| `security.jwt.header` | String | `Authorization` | 讀取 token 的 HTTP 標頭名稱 |
| `security.jwt.token-prefix` | String | `Bearer ` | token 前綴（注意尾端含一個空格） |
| `security.jwt.issuer` | String | 無 | 選填，寫入 token 的 `iss` claim |

## LDAP 子屬性（`security.ldap.*`）

僅在 `security.verification-type: ldap` 時需要設定。

| 屬性鍵 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `security.ldap.ip` | String | 無 | LDAP / AD 伺服器 IP 或主機名稱 |
| `security.ldap.domain` | String | 無 | 網域名稱；登入帳號若不含 `@` 則自動補全為 `userId@domain` |
| `security.ldap.port` | String | 無 | LDAP 埠號（標準 LDAP：389；LDAPS：636） |
| `security.ldap.dn` | String | `DC=zipe,DC=local` | 搜尋起始 DN，如 `DC=corp,DC=example,DC=com` |

## 完整 application.yml 範例

### BASIC 模式（內建 admin/admin，開發測試用）

未設定 `security.basic.users` 時，帳號密碼 fallback 為內建的 `admin`／`admin`，僅適合快速驗證功能。

```yaml
security:
  enable: true
  verification-type: basic
  login-uri: /login
  login-success-uri: /dashboard
  login-failure-uri: /login
  allow-uris: /static/**,/public/**,/resources/**
  csrf-enabled: false
```

### BASIC 模式（自訂帳密）

設定 `security.basic.users` 後，由清單載入帳號，取代內建的 `admin/admin`。密碼可填明文或帶 `{id}` 前綴的預雜湊值：

```yaml
security:
  enable: true
  verification-type: basic
  allow-uris: /static/**,/public/**,/resources/**
  basic:
    users:
      - username: user01
        password: 1234                      # 明文，啟動時自動以 BCrypt 編碼
        authorities: [admin, viewer]
      - username: user02
        password: '{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'  # 預雜湊
        authorities: [viewer]
```

:::tip 正式環境請勿在設定檔寫死明文密碼
建議改填 `{bcrypt}` 前綴的預雜湊值（可用 Spring 的 `PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("...")` 產生），或覆寫 `basicUserServiceImpl` Bean 改由資料庫等外部來源載入。
:::

### LDAP 模式

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
    ip: 192.168.1.100
    domain: corp.example.com       # 帳號自動補全：john → john@corp.example.com
    port: 389
    dn: DC=corp,DC=example,DC=com  # 搜尋起始 DN
```

### CUSTOM 模式（業務資料庫帳號）

```yaml
security:
  enable: true
  verification-type: custom
  custom-bean-name: dbAuthProvider   # 對應業務專案的 @Component("dbAuthProvider")
  login-uri: /login
  login-success-uri: /dashboard
  login-failure-uri: /login
  allow-uris: /static/**,/public/**,/resources/**
  csrf-enabled: false
```

### JWT 無狀態登入（疊加於任一憑證來源）

JWT 與 `verification-type` 正交：`verification-type` 決定怎麼驗帳密，`jwt.enabled: true` 決定驗完改發 token。以下示範 BASIC + JWT（HS256）：

```yaml
security:
  enable: true
  verification-type: basic       # 憑證來源：basic / ldap / custom 皆可
  allow-uris: /static/**,/public/**
  jwt:
    enabled: true                # 啟用 JWT 無狀態登入
    algorithm: HS256
    secret: 0123456789-0123456789-0123456789-secret  # 至少 32 位元組，請以環境變數注入
    expiration-seconds: 3600
    login-uri: /api/login
```

LDAP + JWT（RS256）—— 登入走 LDAP 驗證，驗成功後發 RS256 token：

```yaml
security:
  enable: true
  verification-type: ldap        # 登入以 LDAP 驗帳密
  allow-uris: /static/**,/public/**
  jwt:
    enabled: true
    algorithm: RS256
    private-key-location: classpath:keys/jwt-private.pem
    public-key-location: classpath:keys/jwt-public.pem
    expiration-seconds: 3600
  ldap:
    ip: 192.168.1.100
    domain: corp.example.com
    port: 389
    dn: DC=corp,DC=example,DC=com
```

:::note JWT 啟用時不使用表單登入與 session
`security.jwt.enabled=true` 時模組改用無狀態 filter chain（`SessionCreationPolicy.STATELESS`、停用 CSRF），不套用 `login-uri` 表單登入頁與三個登入 Handler。登入改走內建 `POST /api/login`（路徑由 `security.jwt.login-uri` 設定），帳密驗證依 `verification-type` 走 BASIC / LDAP / CUSTOM；未通過驗證的請求回傳 401。
:::

:::warning LDAP / CUSTOM + JWT 的權限查詢
JWT 每次請求以 token 內的 username 透過 `UserDetailsService` 查權限。`LdapUserDetailsService` 與一般 CUSTOM `AuthenticationProvider` 需要密碼、無法僅以 username 查詢，因此 `verification-type=ldap|custom` 搭配 JWT 時，請覆寫 `basicUserServiceImpl` Bean，提供可依 username 載入權限的 `UserDetailsService`，否則一般使用者帶 token 的請求會因查無使用者而被拒（登入本身仍正常）。
:::

### 含稽核日誌的完整範例

```yaml
security:
  enable: true
  verification-type: custom
  custom-bean-name: dbAuthProvider
  login-uri: /login
  login-success-uri: /dashboard
  login-failure-uri: /login
  allow-uris: /static/**,/public/**,/resources/**
  csrf-enabled: false
  record-log-enable: true
  custom-record-log-bean: auditLogRecord  # 對應業務專案的 @Component("auditLogRecord")
  ldap:
    ip: 192.168.1.100
    domain: corp.example.com
    port: 389
    dn: DC=corp,DC=example,DC=com
```

## 屬性說明補充

### security.enable

設為 `false` 時，`SecurityConfiguration` 仍會建立所有 Bean 並啟動 Spring Security，但 `filterChain` 會對所有路徑（`/**`）呼叫 `.permitAll()`，效果等同於關閉驗證。適合在開發環境暫時停用安全管控。

```yaml
security:
  enable: false  # 全路徑免驗證，上線前務必移除
```

### security.login-uri 與 Session 策略的關係

| 設定值 | 採用設定方法 | Session 策略 |
|---|---|---|
| 有填值（如 `/login`） | `customLoginConfigure()` | `STATELESS` |
| 空白或未設定 | `basicLoginConfigure()` | `Stateful`（Spring Security 預設） |

:::warning STATELESS 模式的注意事項
採用 `customLoginConfigure()` 時 Session 策略為 `STATELESS`，Spring Security 不主動建立 `HttpSession`。若業務邏輯依賴 Session 儲存資料（例如透過 `SecurityBaseService.fetchLoginUser()` 取回 `SysUserVO`），需確保應用層面有另行管理 Session，或改用 `basicLoginConfigure()`（不填 `login-uri`）。
:::

### security.login-failure-uri 的 redirect 行為

`LoginFailureHandler` 設定 `useForward=false`，登入失敗後採**客戶端 redirect**（302）導向 `login-failure-uri`。這代表：

- 瀏覽器以 GET 重新請求失敗頁，URL 會變更為 `login-failure-uri`
- 搭配 Spring Security 預設登入頁時，`login-failure-uri` 請設為 `/login`（即登入頁本身）。**不要設 `/login?error`**：本模組以自訂 `LoginFailureHandler` 取代 formLogin 內建失敗 URL，`DefaultLoginPageGeneratingFilter` 只認得 `/login`、不認得 `/login?error`，後者不會被 render（未放行→被導回 `/login`；放行→落到靜態資源處理器拋 404）。需要在失敗頁顯示錯誤訊息，請改用**自訂登入頁**（`security.login-uri`）

:::danger 不可使用 forward 回 `/login`
請勿改回 `setUseForward(true)`：Spring Security 6/7 會對 `FORWARD` 派發套用 filter chain，若 `login-failure-uri` 與表單登入處理路徑（預設 `/login`）相同，forward 會把失敗的 `POST /login` 再次送進認證 filter，無限轉發直至 `StackOverflowError`。詳見 [architecture.md 注意 7](./architecture.md)。
:::

### ADMIN 帳號動態密碼

帳號名稱（不分大小寫）等於 `admin` 時，由 `CommonLoginProcess.verifySpecialUser()` 處理。動態密碼為**當天日期**，格式 `yyyyMMdd`，例如今天為 2026-06-04，密碼即為 `20260604`。

此機制適用於維護期間系統管理員緊急登入，**業務正常帳號請勿使用 `admin` 作為帳號名稱**。

:::warning CUSTOM 模式必填屬性
設定 `verification-type: custom` 時，**必須同時設定 `custom-bean-name`**，否則 Spring Context 啟動時會因 `NullPointerException` 而失敗，應用無法啟動。
:::

:::warning JWT HS256 金鑰長度
`algorithm=HS256` 時 `security.jwt.secret` 長度**至少需 32 位元組（256 bit）**，否則 jjwt 會在 `JwtTokenProvider.init()` 啟動階段拋出例外。RS256 模式則需同時提供 `private-key-location` 與 `public-key-location`，缺一即啟動失敗。
:::

:::note 密碼編碼
模組內建 `passwordEncoder` Bean，採 `PasswordEncoderFactories.createDelegatingPasswordEncoder()`（委派式編碼器）：依密碼的 `{id}` 前綴（如 `{bcrypt}`）選擇對應演算法比對，並以 BCrypt 編碼新密碼，對既有的 BCrypt 雜湊完全相容。如此 `security.basic.users` 可同時接受明文與帶前綴的預雜湊密碼。CUSTOM 模式的業務 `AuthenticationProvider` 可直接注入此 Bean 進行密碼比對：

```java
@Autowired
private PasswordEncoder passwordEncoder;

// 比對使用者輸入密碼與儲存的雜湊（儲存值建議帶 {bcrypt} 前綴）
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```
:::
