---
id: index
title: logon-spring-boot-starter
sidebar_position: 1
description: 整合 Spring Security 的登入認證 Starter，支援表單登入、LDAP 與自訂驗證
---

# logon-spring-boot-starter

`logon-spring-boot-starter` 整合 Spring Security，提供開箱即用的登入認證流程。模組支援三種驗證類型：資料庫帳號（DB）、LDAP 目錄服務，以及自訂驗證（Custom），並可透過 `VerificationTypeEnum` 切換。內建登入成功／失敗與登出處理器，並提供自訂登入日誌記錄介面，方便整合稽核需求。

## 功能概述

模組以 `SecurityConfiguration` 為核心，自動配置 Spring Security 過濾鏈、表單登入端點與處理器。`CommonLoginProcess` 統一驗證流程，依設定的驗證類型委派至 `BasicUserServiceImpl`（DB）或 `LdapUserDetailsService`（LDAP）。

## 主要特性

- **多種驗證類型**：DB、LDAP、Custom 可切換。
- **表單登入**：內建標準的 Spring Security 表單登入流程。
- **LDAP 整合**：連接企業 LDAP / AD 進行帳號驗證。
- **登入處理器**：`LoginSuccessHandler`、`LoginFailureHandler`、`LogoutSuccessHandler`。
- **登入日誌介面**：實作 `CustomLogonLogRecord` 即可記錄登入稽核軌跡。
- **使用者資訊工具**：`UserInfoUtil` 取得當前登入使用者。

## Maven 依賴引入

```xml
<dependency>
    <groupId>com.zipe</groupId>
    <artifactId>logon-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `logon-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

| 類別 | 職責 |
|---|---|
| `SecurityConfiguration` | Spring Security 主設定，配置過濾鏈與登入端點 |
| `CommonLoginProcess` | 共用登入處理流程 |
| `BasicUserServiceImpl` | 基於資料庫的使用者驗證服務 |
| `LdapUserDetailsService` | 基於 LDAP 的使用者驗證服務 |
| `VerificationTypeEnum` | 驗證類型列舉（DB / LDAP / Custom） |
| `LoginSuccessHandler` | 登入成功處理器 |
| `LoginFailureHandler` | 登入失敗處理器 |
| `LogoutSuccessHandler` | 登出成功處理器 |
| `CustomLogonLogRecord` | 自訂登入日誌記錄介面 |
| `UserInfoUtil` | 取得當前登入使用者資訊 |
| `SysUserVO` | 系統使用者 View Object |

## 快速導航

- [快速開始](./quickstart.md)：啟用表單登入並驗證。
- [配置參考](./configuration.md)：Security 與 LDAP 屬性設定。
- [使用範例](./examples.md)：自訂處理器與登入日誌的實作範例。

:::tip 驗證類型選擇
若您的組織已建置 AD / LDAP，建議直接使用 LDAP 驗證；若帳號自管於資料庫，則使用 DB 驗證；兩者皆不符時，可實作 Custom 驗證。
:::
