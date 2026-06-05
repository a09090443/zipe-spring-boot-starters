# logon-spring-boot-starter

Spring Security 認證整合模組，支援一般表單登入、LDAP 驗證與自訂驗證流程三種模式。

## 主要功能

- 三種驗證模式：`BASIC`（表單）、`LDAP`（目錄服務）、`CUSTOM`（自訂）
- Session 管理與多 Session 控制
- 登入成功／失敗／登出事件處理器
- 自訂登入日誌記錄介面（`CustomLogonLogRecord`）
- 可配置允許匿名存取的 URI 白名單

## 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>logon-spring-boot-starter</artifactId>
    <version>3.5.11.0</version>
</dependency>
```

## 基本設定

```properties
# 啟用安全控制
security.enable=true

# 驗證類型：BASIC、LDAP、CUSTOM
security.verification-type=BASIC

# 自訂登入頁面（選填）
security.login-uri=/login

# 不需認證的 URI 白名單
security.allow-uris=/public/**,/static/**

# CSRF 開關
security.csrf-enabled=false

# LDAP 設定（使用 LDAP 模式時）
ldap.url=ldap://localhost:389
ldap.base=dc=example,dc=com
```
