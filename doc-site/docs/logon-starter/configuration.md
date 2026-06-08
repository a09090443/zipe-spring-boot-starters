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
| `security.verification-type` | String | `basic` | 驗證模式：`basic` / `ldap` / `custom`（大小寫不敏感） |
| `security.login-uri` | String | `/login` | 自訂登入頁路徑；設定此值時採用 `customLoginConfigure`（STATELESS Session）；若留空則使用 Spring Security 預設登入頁（Stateful Session） |
| `security.login-success-uri` | String | `/dashboard` | 登入成功後的導向路徑 |
| `security.login-failure-uri` | String | `/login` | 登入失敗後的目標路徑（採用伺服器端 forward，瀏覽器 URL 不改變） |
| `security.allow-uris` | String | 無 | 免驗證放行的路徑，逗號分隔，支援 Ant 樣式，如 `/static/**,/public/**` |
| `security.csrf-enabled` | Boolean | `true` | CSRF 保護開關；傳統表單應維持 `true`，純 REST API 視情況可設為 `false` |
| `security.frame-options-mode` | Enum | `SAMEORIGIN` | X-Frame-Options（點擊劫持防護）模式：`SAMEORIGIN`（僅同源可內嵌）/ `DENY`（禁止內嵌）/ `DISABLE`（停用，不建議） |
| `security.record-log-enable` | Boolean | `false` | 是否啟用登入稽核回呼；設為 `true` 時必須同時設定 `custom-record-log-bean` |
| `security.custom-record-log-bean` | String | 無 | `record-log-enable=true` 時必填；業務端 `CustomLogonLogRecord` Bean 的名稱 |
| `security.custom-bean-name` | String | 無 | `verification-type=custom` 時必填；業務端 `AuthenticationProvider` Bean 的名稱 |

## LDAP 子屬性（`security.ldap.*`）

僅在 `security.verification-type: ldap` 時需要設定。

| 屬性鍵 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `security.ldap.ip` | String | 無 | LDAP / AD 伺服器 IP 或主機名稱 |
| `security.ldap.domain` | String | 無 | 網域名稱；登入帳號若不含 `@` 則自動補全為 `userId@domain` |
| `security.ldap.port` | String | 無 | LDAP 埠號（標準 LDAP：389；LDAPS：636） |
| `security.ldap.dn` | String | `DC=zipe,DC=local` | 搜尋起始 DN，如 `DC=corp,DC=example,DC=com` |

## 完整 application.yml 範例

### BASIC 模式（開發測試用）

帳號固定為 `admin`，密碼固定為 `admin`，僅適合快速驗證功能。

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

### security.login-failure-uri 的 forward 行為

`LoginFailureHandler` 設定 `useForward=true`，登入失敗後採**伺服器端 forward** 至 `login-failure-uri`，而非客戶端 redirect。這代表：

- 瀏覽器 URL 不會改變（仍顯示登入表單送出的 URL）
- 可在 `login-failure-uri` 對應的 Controller / View 中存取 `AuthenticationException`，用來顯示錯誤訊息

### ADMIN 帳號動態密碼

帳號名稱（不分大小寫）等於 `admin` 時，由 `CommonLoginProcess.verifySpecialUser()` 處理。動態密碼為**當天日期**，格式 `yyyyMMdd`，例如今天為 2026-06-04，密碼即為 `20260604`。

此機制適用於維護期間系統管理員緊急登入，**業務正常帳號請勿使用 `admin` 作為帳號名稱**。

:::warning CUSTOM 模式必填屬性
設定 `verification-type: custom` 時，**必須同時設定 `custom-bean-name`**，否則 Spring Context 啟動時會因 `NullPointerException` 而失敗，應用無法啟動。
:::

:::note 密碼編碼
模組內建 `BCryptPasswordEncoder`（Bean 名稱 `passwordEncoder`），BASIC 模式的密碼比對使用 BCrypt。CUSTOM 模式的業務 `AuthenticationProvider` 可直接注入此 Bean 進行密碼比對：

```java
@Autowired
private PasswordEncoder passwordEncoder;

// 比對使用者輸入密碼與資料庫儲存的 BCrypt hash
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```
:::
