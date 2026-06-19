# iam-spring-boot-starter

身分與授權管理模組，提供可儲存於資料庫的**帳號（Account）— 群組（Group）— 權限（Permission）**模型，並透過 logon-starter 的 `GrantedAuthoritiesResolver` 擴充點接上登入流程，讓業務系統具備集中式的帳號管理與權限控管能力。

## 主要功能

- 帳號／群組／權限三層模型（群組即角色，群組持有多個權限，帳號加入多個群組）
- 泛用具名權限：權限為抽象授權點（`code`），**不綁定 HTTP method / URL**，由開發者自行決定使用方式
- 與 logon 無縫整合：BASIC / LDAP / CUSTOM 三種驗證模式皆共用同一套 DB 授權來源
- BASIC / JWT 模式以資料庫帳號取代 logon 內建的 `admin/admin` stub
- 內建帳號／群組／權限 REST CRUD 端點（可選、可關閉、可覆寫）
- 全面 `@ConditionalOnMissingBean`：Service / Resolver / UserDetailsService / Controller 皆可覆寫
- 服務層回傳 VO，不外漏 JPA 實體與 lazy proxy
- 附 `schema-iam.sql`（相容 H2 / MySQL / PostgreSQL），預設不自動建表

## 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>iam-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

> 本模組相依 `logon-spring-boot-starter` 與 `spring-boot-starter-data-jpa`，引入前請於專案根目錄執行 `mvn clean install`。

## 基本設定

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:iam;DB_CLOSE_DELAY=-1
    username: sa
    password:

iam:
  enabled: true            # iam 總開關
  api:
    enabled: true          # 內建 REST Controller 開關
    base-path: /api/iam    # 內建 API 路由前綴
  group:
    role-prefix: ROLE_     # 群組 code 轉 authority 的前綴
  ddl:
    init: false            # 是否由 schema-iam.sql 自動建表
```

## 與 logon 的整合

| 接點 | logon 預設 | iam 覆寫 | 效果 |
|---|---|---|---|
| `GrantedAuthoritiesResolver` | 回空集合 | `DbGrantedAuthoritiesResolver` | 三種驗證模式皆由 DB 取得 authorities |
| `BasicUserServiceImpl` | `admin/admin` stub | `IamUserDetailsService` | BASIC / JWT 登入帳號改由 `iam_account` 表提供 |

完整說明請參閱 [doc-site 技術文件](../doc-site/docs/iam-starter/index.md)。
