---
id: architecture
title: 架構與開發指南
sidebar_position: 5
---

# 架構與開發指南

本文件面向需要維護、擴充或深入理解 `iam-spring-boot-starter` 的開發人員，涵蓋套件結構、資料模型、與 logon-starter 的整合機制、自動配置原理，以及擴充時的步驟與陷阱。

---

## 1. 模組定位與設計理念

### 定位

`iam-spring-boot-starter` 是一個 **Spring Boot Auto-Configuration 模組**，提供可儲存於資料庫的帳號／群組／權限模型，並透過 logon-starter 的擴充點接上登入流程，讓業務系統獲得集中式的身分與授權管理能力。

### 設計理念

| 理念 | 說明 |
|---|---|
| **核心精簡、客製必備** | 核心僅帳號／群組／權限三組 Service；內建 REST Controller 為可選成品，所有環節皆可覆寫 |
| **授權與認證分離** | 認證（誰來驗帳密）交由 logon 的 verification-type 決定；授權（有哪些權限）統一由 iam 的 `GrantedAuthoritiesResolver` 提供 |
| **泛用具名權限** | 權限是抽象授權點（`code`），不綁定 HTTP method／URL，由開發者自行決定使用方式 |
| **DTO 邊界** | 服務層一律回傳 VO，不外漏 JPA 實體與 lazy proxy |
| **Bean 覆寫友善** | 所有 `@Bean` 均 `@ConditionalOnMissingBean`，宣告同型別 Bean 即可覆寫，不需 `spring.main.allow-bean-definition-overriding` |

### 與 logon-starter 的依賴方向

依賴方向為 **iam → logon**（單向）。logon 定義授權解析的 SPI 介面 `GrantedAuthoritiesResolver` 並提供預設空實作以保持向後相容；iam 相依 logon 並以 `DbGrantedAuthoritiesResolver` 覆寫之。logon 不知道 iam 的存在，未引入 iam 時行為與現況完全一致。

---

## 2. 套件結構

```
iam-spring-boot-starter/
└── src/main/java/com/zipe/
    ├── autoconfiguration/
    │   └── IamAutoConfiguration.java     # @AutoConfiguration 唯一入口（含巢狀 Controller 配置）
    ├── config/
    │   └── IamProperties.java            # iam.* 屬性綁定
    ├── entity/
    │   ├── Account.java                  # iam_account，與 Group 多對多
    │   ├── Group.java                    # iam_group，與 Permission 多對多
    │   └── Permission.java               # iam_permission
    ├── repository/
    │   ├── AccountRepository.java        # findByUsername 回 Optional
    │   ├── GroupRepository.java
    │   └── PermissionRepository.java
    ├── service/
    │   ├── AccountService.java / AccountServiceImpl.java
    │   ├── GroupService.java / GroupServiceImpl.java
    │   └── PermissionService.java / PermissionServiceImpl.java
    ├── security/
    │   ├── DbGrantedAuthoritiesResolver.java  # 實作 logon SPI
    │   └── IamUserDetailsService.java         # 繼承 logon BasicUserServiceImpl
    ├── controller/
    │   ├── AccountController.java
    │   ├── GroupController.java
    │   └── PermissionController.java
    └── vo/
        ├── AccountVO / GroupVO / PermissionVO
        ├── CreateAccountRequest / UpdateAccountRequest
        └── CreateGroupRequest / CreatePermissionRequest
└── src/main/resources/
    ├── schema-iam.sql                    # 五張 iam_ 表 DDL
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 3. 資料模型

帳號（Account）─群組（Group，即角色）─權限（Permission）三層模型，兩兩多對多：

```
Account ──< iam_account_group >── Group ──< iam_group_permission >── Permission
```

- **Account**：登入帳號核心資料（username、BCrypt password、enabled、locked、稽核時間），與 Group 多對多。
- **Group**：即「角色」，`code` 全表唯一，套 `role-prefix` 後成為 `ROLE_*` authority；持有多個 Permission。
- **Permission**：泛用具名授權點，`code` 全表唯一、原樣作為 authority。

實體的 `equals`／`hashCode` 以資料庫 `id` 比較，並以 `Hibernate.getClass()` 區分代理類別與真實類別，避免 Hibernate Proxy 誤判（與 `starters_example` 既有實體一致）。

---

## 4. 與 logon-starter 的整合機制

這是本模組的核心。logon 在 `com.zipe.security` 套件定義 SPI：

```java
public interface GrantedAuthoritiesResolver {
    Collection<? extends GrantedAuthority> resolve(String username);
}
```

logon 於 `SecurityConfiguration` 提供預設 `@Bean`（`@ConditionalOnMissingBean`），回傳 `username -> Collections.emptyList()`，保留「無權限」的現行行為。`LdapUserDetailsService` 的 `buildAuthenticatedToken(principal)` 改以此 resolver 解析 authorities。

iam 在兩個接點覆寫 logon 預設：

| 接點 | logon 預設 | iam 覆寫 | 效果 |
|---|---|---|---|
| **授權** `GrantedAuthoritiesResolver` | 回空集合 | `DbGrantedAuthoritiesResolver` | 三種驗證模式（BASIC／LDAP／CUSTOM）皆由 DB 取得 authorities |
| **認證** `BasicUserServiceImpl` | 寫死 `admin/admin` stub | `IamUserDetailsService`（繼承之） | BASIC／JWT 模式登入帳號改由 `iam_account` 表提供 |

兩者皆以 `@ConditionalOnMissingBean` 註冊：iam 的 Bean 以**父型別／介面型別**宣告（`BasicUserServiceImpl`、`GrantedAuthoritiesResolver`），使 logon 同型別的預設 Bean 自動退讓。

### 授權解析流程

```
登入成功 → resolver.resolve(username)
        → AccountRepository.findByUsername
        → 帳號的每個 Group：加入 SimpleGrantedAuthority(role-prefix + group.code)
        → 每個 Group 的每個 Permission：加入 SimpleGrantedAuthority(permission.code)
        → 去重（LinkedHashSet）回傳
```

解析以 `@Transactional(readOnly = true)` 包覆，於交易內讀取 lazy 關聯以避免 `LazyInitializationException`；查無帳號回空集合（不回 `null`）。

---

## 5. 自動配置原理

`IamAutoConfiguration` 標註：

- `@AutoConfiguration` + `META-INF/spring/...AutoConfiguration.imports` 註冊，引入即生效。
- `@EnableConfigurationProperties(IamProperties.class)`：綁定 `iam.*`。
- `@ConditionalOnProperty(prefix="iam", name="enabled", havingValue="true", matchIfMissing=true)`：`iam.enabled=false` 時整組停用。
- `@EntityScan("com.zipe.entity")` + `@EnableJpaRepositories("com.zipe.repository")`：掃描 starter 套件，業務專案無須自行設定掃描路徑。

各 `@Bean`（`AccountService`／`GroupService`／`PermissionService`／`GrantedAuthoritiesResolver`／`BasicUserServiceImpl`）皆 `@ConditionalOnMissingBean`，以建構子注入對應 Repository；`PasswordEncoder` 由方法參數注入（取 logon 容器提供的 Bean）。

內建 Controller 置於巢狀靜態 `@Configuration`（`IamControllerConfiguration`），以 `@ConditionalOnProperty(prefix="iam.api", name="enabled", matchIfMissing=true)` 控制整組裝配，並對每個 Controller `@Bean` 標 `@ConditionalOnMissingBean`。Controller 的路由前綴採類別層級 `@RequestMapping("${iam.api.base-path:/api/iam}/...")` 屬性佔位符，於 handler 註冊時由 Environment 解析，使 `iam.api.base-path` 真正生效。

:::warning 引入 iam 後，應用須自行宣告 @EnableJpaRepositories
`IamAutoConfiguration` 帶有 `@EnableJpaRepositories("com.zipe.repository")` 以自我註冊 iam 的 Repository。依 Spring Boot 規則，**容器中只要出現任何顯式 `@EnableJpaRepositories`，框架對應用主套件的 JPA Repository 自動掃描即退讓**。因此原本僅靠 `@SpringBootApplication` 自動掃描 Repository 的應用，引入 iam 後其自身 Repository 會註冊失敗（啟動時報 `No qualifying bean of type ...Repository`）。解法是在應用啟動類別**顯式宣告自身的** `@EnableJpaRepositories(basePackages = "你的.repository.套件")`，與 iam 的 `com.zipe.repository` 各自獨立掃描、互不影響。
:::

:::caution Spring Boot 4.0.0 套件位置
本專案執行於 Spring Boot 4.0.0，部分 auto-configuration 已模組化。`@EntityScan` 位於 `org.springframework.boot.persistence.autoconfigure`（非舊有的 `org.springframework.boot.autoconfigure.domain`）；撰寫測試時 `HibernateJpaAutoConfiguration` 位於 `org.springframework.boot.hibernate.autoconfigure`、`DataSourceAutoConfiguration` 位於 `org.springframework.boot.jdbc.autoconfigure`，且 `@DataJpaTest` 等測試切片已不在 classpath，需改用 `@SpringBootTest` 或 `ApplicationContextRunner`。
:::

---

## 6. 擴充指南

| 需求 | 做法 |
|---|---|
| 調整帳號→authorities 規則 | 宣告 `GrantedAuthoritiesResolver` Bean，覆寫 `DbGrantedAuthoritiesResolver` |
| 帳號來源非 iam 表 | 宣告 `BasicUserServiceImpl` Bean（繼承之、覆寫 `loadUserByUsername`），authorities 仍可用 resolver |
| 客製某個 Service 行為 | 宣告同型別 `AccountService`／`GroupService`／`PermissionService` Bean |
| 替換／關閉內建 API | 覆寫對應 Controller Bean，或 `iam.api.enabled=false` 後自行實作 |
| 調整 API 路徑 | 設定 `iam.api.base-path` |
| 群組前綴非 `ROLE_` | 設定 `iam.group.role-prefix` |

---

## 7. 測試

starter 的測試（`src/test`）涵蓋四個面向，皆於 H2／記憶體環境離線執行：

1. **Repository 層**：`@SpringBootTest` + H2，驗證帳號／群組／權限 CRUD 與多對多關聯查詢（`findByUsername`、帳號取群組與權限）。
2. **DbGrantedAuthoritiesResolver 單元測試**：以 Mockito 驗證 authorities 展開規則（群組帶前綴、權限原樣、共用權限去重、查無帳號回空）。
3. **整合測試**：`@SpringBootTest` 驗證 `IamUserDetailsService` 由 iam 帳號表載入 `UserDetails`，及覆寫 `GrantedAuthoritiesResolver` Bean 時 iam 預設退讓（`ApplicationContextRunner`）。
4. **logon 增補**：以 stub resolver 驗證 `LdapUserDetailsService.buildAuthenticatedToken` 會把 authorities 注入已認證 token（不需真連 LDAP）。
