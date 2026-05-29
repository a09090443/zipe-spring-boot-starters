---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `logon-spring-boot-starter` 的可設定屬性，分為 Security 設定（`zipe.security.*`）與 LDAP 設定（`zipe.ldap.*`）兩大類。

## Security 屬性

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.security.verification-type` | String | `DB` | 驗證類型：`DB` / `LDAP` / `CUSTOM` | 否 |
| `zipe.security.login-page` | String | `/login` | 登入頁路徑 | 否 |
| `zipe.security.login-process-url` | String | `/doLogin` | 表單送出的驗證端點 | 否 |
| `zipe.security.default-success-url` | String | `/` | 登入成功預設導向頁 | 否 |
| `zipe.security.logout-url` | String | `/logout` | 登出端點 | 否 |
| `zipe.security.username-parameter` | String | `username` | 帳號欄位名稱 | 否 |
| `zipe.security.password-parameter` | String | `password` | 密碼欄位名稱 | 否 |
| `zipe.security.permit-all` | List | `[]` | 免驗證放行的路徑清單 | 否 |

## LDAP 屬性

當 `verification-type` 為 `LDAP` 時需提供：

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.ldap.url` | String | 無 | LDAP 伺服器位址（如 `ldap://host:389`） | 是（LDAP 模式） |
| `zipe.ldap.base-dn` | String | 無 | 搜尋的基礎 DN | 是（LDAP 模式） |
| `zipe.ldap.user-dn-pattern` | String | 無 | 使用者 DN 樣式 | 否 |
| `zipe.ldap.manager-dn` | String | 無 | 管理者繫結 DN | 否 |
| `zipe.ldap.manager-password` | String | 無 | 管理者密碼 | 否 |
| `zipe.ldap.user-search-filter` | String | `(uid={0})` | 使用者搜尋過濾條件 | 否 |

## 完整 application.yml 範例

以下範例同時展示 Security 與 LDAP 設定：

```yaml
zipe:
  security:
    verification-type: LDAP
    login-page: /login
    login-process-url: /doLogin
    default-success-url: /home
    logout-url: /logout
    username-parameter: username
    password-parameter: password
    permit-all:
      - /login
      - /css/**
      - /js/**
      - /images/**
  ldap:
    url: ldap://ldap.example.com:389
    base-dn: dc=example,dc=com
    user-dn-pattern: uid={0},ou=people
    manager-dn: cn=admin,dc=example,dc=com
    manager-password: ${LDAP_MANAGER_PASSWORD}
    user-search-filter: (uid={0})
```

:::warning 驗證類型必須與設定一致
若 `verification-type` 設為 `LDAP` 卻未提供 `zipe.ldap.url` 等必填屬性，應用啟動時將因無法建立 LDAP 連線而失敗。請確保所選驗證類型的相依設定齊備。
:::

:::note 自訂驗證（CUSTOM）
選用 `CUSTOM` 時，模組不會提供預設的驗證實作，您需自行提供 `UserDetailsService` 或 `AuthenticationProvider` Bean，並交由 `CommonLoginProcess` 整合。
:::

:::info 密碼編碼
資料庫驗證的密碼比對依賴 Spring Security 的 `PasswordEncoder`。請確認資料庫中儲存的密碼為編碼後（如 BCrypt）的雜湊值，而非明文。
:::
