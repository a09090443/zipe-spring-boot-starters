---
id: index
title: logon-spring-boot-starter
sidebar_position: 1
description: 整合 Spring Security 的登入認證 Starter，支援表單登入、LDAP 與自訂驗證
---

# logon-spring-boot-starter

`logon-spring-boot-starter` 整合 Spring Security，提供開箱即用的登入認證流程。模組支援三種驗證類型：BASIC（內建 stub）、LDAP 目錄服務，以及 CUSTOM（業務自訂 `AuthenticationProvider`），透過 `security.verification-type` 屬性切換，無需修改程式碼。內建登入成功／失敗與登出三種 Handler，並提供 `CustomLogonLogRecord` 介面供業務專案實作登入稽核日誌。

## 功能概述

模組以 `SecurityConfiguration`（`@AutoConfiguration`）為唯一入口，自動配置 Spring Security 過濾鏈、表單登入端點與三個生命週期 Handler。`CommonLoginProcess` 提供統一的 `AuthenticationProvider` 骨架（含 ADMIN 動態密碼機制），子類別覆寫 `verifyNormalUser()` 即可實作不同驗證邏輯。LDAP 模式由 `LdapUserDetailsService` 實作，透過 `base-spring-boot-starter` 提供的 `LdapUtil` 與 Active Directory / LDAP 互動；CUSTOM 模式由業務專案自行提供 Bean。

## 主要特性

- **多種驗證類型**：`BASIC`（開發用 stub）、`LDAP`（企業 AD）、`CUSTOM`（業務資料庫或任意邏輯）可切換。
- **表單登入**：內建標準的 Spring Security 表單登入流程，支援自訂或預設登入頁。
- **LDAP 整合**：自動補全網域、JNDI 連線 AD/LDAP，驗證後解析 `sAMAccountName`。
- **ADMIN 動態密碼**：帳號 `admin` 以當日日期（`yyyyMMdd`）作為動態密碼，便於維護期間緊急登入。
- **登入 Handler**：`LoginSuccessHandler`（還原原始請求 URL）、`LoginFailureHandler`（伺服器端 forward）、`LogoutSuccessHandler`（清理 Session）。
- **登入日誌介面**：實作 `CustomLogonLogRecord` 並設定 `security.record-log-enable: true` 即可記錄登入稽核軌跡。
- **靜態工具**：`UserInfoUtil.loginUserId()` 從 `SecurityContextHolder` 取得當前登入者 ID。
- **安全開關**：`security.enable: false` 可全路徑放行，適合開發測試環境。

## Maven 依賴引入

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>logon-spring-boot-starter</artifactId>
    <version>4.0.0.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `logon-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

:::warning 不得自行定義 SecurityFilterChain
模組無條件配置 `SecurityFilterChain`，業務專案**不得重複定義**此 Bean，否則 Spring Context 啟動時將因 Bean 定義衝突而失敗。若需調整過濾鏈行為，請使用屬性設定或覆寫個別 Handler Bean。
:::

## 主要類別

| 類別 | 套件 | 職責 |
|---|---|---|
| `SecurityConfiguration` | `autoconfiguration` | `@AutoConfiguration` 唯一入口，配置 `SecurityFilterChain` 與所有 Bean |
| `SecurityPropertyConfig` | `config` | 屬性綁定（prefix: `security`），持有所有 Security 設定 |
| `LdapPropertyConfig` | `config` | 屬性綁定（prefix: `security.ldap`），持有 LDAP 連線設定 |
| `CommonLoginProcess` | `service` | `AuthenticationProvider` 抽象骨架，統一登入流程與 ADMIN 動態密碼 |
| `BasicUserServiceImpl` | `service` | BASIC 模式的 `UserDetailsService`（hardcoded stub，僅適合開發測試） |
| `LdapUserDetailsService` | `service` | LDAP 模式，繼承 `CommonLoginProcess`，與 AD/LDAP 互動 |
| `CustomLogonLogRecord` | `service` | 稽核日誌回呼介面，業務專案自行實作 |
| `LoginSuccessHandler` | `handler` | 登入成功處理器，記錄日誌與 IP，導向成功頁 |
| `LoginFailureHandler` | `handler` | 登入失敗處理器，分類日誌，伺服器端 forward 至失敗頁 |
| `LogoutSuccessHandler` | `handler` | 登出成功處理器，清理 Session，導向登入頁 |
| `UserInfoUtil` | `util` | 靜態工具，`loginUserId()` 從 `SecurityContextHolder` 取得當前帳號 |
| `SecurityBaseService` | `base/service` | 業務 Service 基底，封裝 Session 存取，`fetchLoginUser()` 回傳 `SysUserVO` |
| `SysUserVO` | `vo` | 儲存於 `HttpSession` 的使用者 VO（`userId` / `loginTime`） |
| `VerificationTypeEnum` | `enums` | 驗證模式列舉：`BASIC` / `LDAP` / `CUSTOM` |
| `UserEnum` | `enums` | 特殊使用者列舉：`SYSTEM` / `ADMIN` |
| `LdapUser` | `model` | LDAP 驗證後的資料傳輸物件 |

## 快速導航

- [快速開始](./quickstart.md)：啟用表單登入並取得當前登入使用者。
- [配置參考](./configuration.md)：`security.*` 與 `security.ldap.*` 所有屬性說明。
- [使用範例](./examples.md)：自訂驗證、稽核日誌與 Handler 覆寫的完整範例。
- [架構與開發指南](./architecture.md)：套件結構、類別詳解、協作流程與擴充指南。

:::tip 驗證類型選擇建議
- 組織已建置 AD / LDAP → 使用 `ldap` 驗證
- 帳號自管於資料庫 → 使用 `custom` 驗證，實作 `CommonLoginProcess.verifyNormalUser()`
- 快速開發 / 測試環境 → 使用 `basic`（注意：預設帳密為 `admin/admin`，上線前必須切換）
:::
