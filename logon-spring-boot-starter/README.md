# logon-spring-boot-starter

Spring Security 認證整合模組，以兩個正交設定組合登入方式：憑證來源（`verification-type`：BASIC / LDAP / CUSTOM）與登入後狀態策略（`security.jwt.enabled`：傳統 session 或 JWT 無狀態 token）。

## 主要功能

- 三種憑證來源：`BASIC`（表單）、`LDAP`（目錄服務）、`CUSTOM`（自訂），由 `verification-type` 切換
- JWT 無狀態登入（正交疊加）：`security.jwt.enabled=true` 即在任一憑證來源之上改發 token；內建 `/api/login`，支援 HS256 / RS256，token 僅存 username、每次查 `UserDetailsService` 取權限
- Session 管理與多 Session 控制
- 登入成功／失敗／登出事件處理器
- 自訂登入日誌記錄介面（`CustomLogonLogRecord`）
- 可配置允許匿名存取的 URI 白名單

## 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>logon-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

## 基本設定

```properties
# 啟用安全控制
security.enable=true

# 憑證來源：BASIC、LDAP、CUSTOM
security.verification-type=BASIC

# JWT 無狀態登入（與 verification-type 正交，可疊加於任一憑證來源）
# security.jwt.enabled=true
# security.jwt.secret=請填入至少 32 位元組的密鑰
# security.jwt.expiration-seconds=3600
# security.jwt.login-uri=/api/login

# 自訂登入頁面（選填）
security.login-uri=/login

# 登出路徑（選填，預設 /logout）。切勿設為 /login，否則會與表單登入處理路徑相撞而使登入失效
# security.logout-uri=/logout

# 不需認證的 URI 白名單
security.allow-uris=/public/**,/static/**

# CSRF 開關
security.csrf-enabled=false

# LDAP 設定（使用 LDAP 模式時）
ldap.url=ldap://localhost:389
ldap.base=dc=example,dc=com
```
