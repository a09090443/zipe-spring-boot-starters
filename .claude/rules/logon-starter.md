---
description: logon-spring-boot-starter 的功能說明與 doc-site 文件導覽
paths:
  - logon-spring-boot-starter/**
---

# logon-spring-boot-starter

登入認證功能的 Starter，整合 Spring Security，支援表單登入（BASIC）、LDAP 驗證、以及自訂驗證流程三種模式。

## 主要功能領域

Spring Security 表單登入、LDAP 目錄服務驗證（`LdapUserDetailsService`）、`VerificationTypeEnum`（BASIC / LDAP / CUSTOM）切換驗證方式、登入成功 / 失敗 / 登出處理器、`CustomLogonLogRecord` 自訂日誌介面、`UserInfoUtil` 取得當前使用者資訊。

## doc-site 文件導覽

工作於本模組時，依需求閱讀對應文件：

| 需求 | doc-site 文件 |
|---|---|
| 了解模組整體功能與主要類別清單 | [index.md](../../doc-site/docs/logon-starter/index.md) |
| 引入 Maven 依賴與 Spring Security 最小設定步驟 | [quickstart.md](../../doc-site/docs/logon-starter/quickstart.md) |
| 查詢 `security.*`（enable、verification-type、allow-uris、login-uri 等）與 `security.ldap.*`（ip、domain、port、dn）屬性，以及 `VerificationTypeEnum` / `FrameOptionsMode` enum 值 | [configuration.md](../../doc-site/docs/logon-starter/configuration.md) |
| 查詢切換驗證類型、實作 `CustomLogonLogRecord` 自訂日誌、`UserInfoUtil.loginUserId()` 取得當前使用者的用法 | [examples.md](../../doc-site/docs/logon-starter/examples.md) |
| 了解 Security Filter Chain 結構、三種驗證流程細節（BASIC / LDAP / CUSTOM）、擴充指南 | [architecture.md](../../doc-site/docs/logon-starter/architecture.md) |
