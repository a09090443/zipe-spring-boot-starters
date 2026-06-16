---
id: index
title: iam-spring-boot-starter
sidebar_position: 1
description: 以資料庫儲存帳號／群組／權限的身分與授權管理 Starter，與 logon-starter 無縫整合
---

# iam-spring-boot-starter

`iam-spring-boot-starter` 提供一套可儲存於資料庫的**帳號（Account）— 群組（Group）— 權限（Permission）**身分與授權模型，讓引入的業務系統具備完整的帳號管理與權限控管能力。模組以 JPA 持久化資料，並透過 logon-starter 的 `GrantedAuthoritiesResolver` 擴充點接上登入流程：登入成功後，使用者的 Spring Security authorities 直接由資料庫的群組與權限展開而來，可搭配 `@PreAuthorize`、`hasRole()`、`hasAuthority()` 等標準機制使用。

模組維持「核心精簡、客製必備」的設計：核心為帳號／群組／權限三組 Service（必備），內建的 REST CRUD Controller 則為可選、可關閉、可覆寫的成品。所有對外 Bean 皆以 `@ConditionalOnMissingBean` 註冊，業務專案宣告同型別 Bean 即可覆寫任一環節。

## 功能概述

模組以 `IamAutoConfiguration`（`@AutoConfiguration`）為唯一入口，透過 `@EntityScan` 與 `@EnableJpaRepositories` 掃描 starter 套件，引入即生效。核心持久層由 `Account`、`Group`、`Permission` 三個 JPA 實體與對應 Repository 組成，帳號↔群組、群組↔權限皆為多對多關聯（`iam_account_group`、`iam_group_permission` 關聯表）。

與 logon-starter 的整合是本模組的關鍵：

- **授權（Authorization）**：`DbGrantedAuthoritiesResolver` 實作 logon 的 `GrantedAuthoritiesResolver` SPI，以 `GrantedAuthoritiesResolver` 型別註冊，覆寫 logon 的預設空實作。**LDAP、BASIC、CUSTOM 三種驗證模式皆共用此同一套授權來源**——無論帳號密碼由誰驗證，權限一律由 iam 資料庫的群組／權限展開。
- **認證（Authentication，BASIC／JWT 模式）**：`IamUserDetailsService` 繼承 logon 的 `BasicUserServiceImpl`，以 `BasicUserServiceImpl` 型別註冊，使 logon 寫死 `admin/admin` 的預設 stub 自動退讓，登入帳號改由 iam 帳號表提供（密碼為 BCrypt 雜湊）。

## 主要特性

- **資料庫帳號模型**：帳號／群組／權限三層模型，群組即角色、群組持有多個權限，帳號加入多個群組。
- **泛用具名權限**：權限是抽象的「具名授權點」（如 `ORDER_EXPORT`、`USER_MANAGE`），**不綁定 HTTP method 或 URL**——開發者依自身程式邏輯決定如何使用，不限於 API 授權。
- **與 logon 無縫整合**：透過 `GrantedAuthoritiesResolver` SPI 接上登入流程，三種驗證模式共用 DB 授權；BASIC／JWT 模式另以資料庫帳號取代內建 stub。
- **內建 REST CRUD（可選）**：提供帳號／群組／權限的 REST 端點，預設啟用、路由前綴可設定，並以 `iam.api.enabled=false` 整組關閉或逐一覆寫。
- **全面可覆寫**：所有 Service、Resolver、UserDetailsService 與 Controller 皆 `@ConditionalOnMissingBean`，客製化不需 `spring.main.allow-bean-definition-overriding`。
- **DTO 邊界**：服務層一律回傳 VO，不外漏 JPA 實體與 lazy proxy；帳號視圖不含密碼雜湊。
- **建表策略可控**：附 `schema-iam.sql`（相容 H2／MySQL／PostgreSQL），預設不自動執行，交由業務專案以 Flyway／Liquibase 或手動掌控。

## Maven 依賴引入

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>iam-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

:::note 安裝前置作業
`iam-spring-boot-starter` 相依於 `logon-spring-boot-starter`。引入前請先於專案根目錄執行 `mvn clean install`，將 logon 與 iam 模組一併安裝至本地 Maven Repository。
:::

:::tip 核心 Service 必備、Controller 可選
引入 starter 後，帳號／群組／權限三組 Service 即自動裝配（必備能力）。內建 REST Controller 預設啟用，可用 `iam.api.enabled=false` 關閉；關閉後三組 Service 仍可注入到業務程式中自行使用。
:::

## 主要類別

| 類別 | 套件 | 職責 |
|---|---|---|
| `IamAutoConfiguration` | `autoconfiguration` | `@AutoConfiguration` 唯一入口，裝配所有 Service／Resolver／UserDetailsService 與 Controller |
| `IamProperties` | `config` | 屬性綁定（prefix: `iam`），持有啟用、API、群組前綴與建表設定 |
| `Account` | `entity` | 帳號實體，對應 `iam_account`，與 `Group` 多對多 |
| `Group` | `entity` | 群組（角色）實體，對應 `iam_group`，與 `Permission` 多對多 |
| `Permission` | `entity` | 權限實體，對應 `iam_permission`，泛用具名授權點 |
| `AccountRepository` | `repository` | 帳號資料存取，含 `findByUsername` 回 `Optional` |
| `GroupRepository` / `PermissionRepository` | `repository` | 群組／權限資料存取 |
| `AccountService` | `service` | 帳號建立／查詢／更新／停用／改密碼／群組指派 |
| `GroupService` | `service` | 群組 CRUD 與群組／權限掛卸 |
| `PermissionService` | `service` | 權限 CRUD 與查詢 |
| `DbGrantedAuthoritiesResolver` | `security` | 實作 logon SPI，由 DB 展開帳號→群組→權限為 authorities |
| `IamUserDetailsService` | `security` | 繼承 `BasicUserServiceImpl`，由 iam 帳號表提供 `UserDetails` |
| `AccountController` / `GroupController` / `PermissionController` | `controller` | 內建可選 REST CRUD 端點 |
| `AccountVO` / `GroupVO` / `PermissionVO` | `vo` | 服務層對外回傳的資料視圖 |
| `CreateAccountRequest` / `UpdateAccountRequest` 等 | `vo` | 建立／更新的請求物件 |

## 快速導航

- [快速開始](./quickstart.md)：建表、建帳號、接上登入並以權限保護端點。
- [配置參考](./configuration.md)：`iam.*` 所有屬性說明與資料表結構。
- [使用範例](./examples.md)：權限保護、覆寫 Resolver、自訂帳號來源的完整範例。
- [架構與開發指南](./architecture.md)：套件結構、與 logon 的整合機制、擴充指南。

:::tip 與 logon-starter 的搭配
- 已建置 AD／LDAP 驗證 → 帳號密碼由 LDAP 驗，**權限由 iam 群組／權限提供**。
- 帳號自管於資料庫 → 使用 BASIC（或加 JWT），帳號與權限皆由 iam 提供。
- 兩者皆透過同一個 `GrantedAuthoritiesResolver` 接點，無需修改 logon 程式碼。
:::
