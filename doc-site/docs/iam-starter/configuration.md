---
id: configuration
title: 配置參考
sidebar_position: 3
description: iam-spring-boot-starter 的 iam.* 設定屬性與資料表結構
---

# 配置參考

`iam-spring-boot-starter` 的所有設定皆以 `iam.*` 前綴綁定至 `IamProperties`。各屬性皆有合理預設值，**最小設定可不填任何屬性**即引入生效。

## 設定屬性

| 屬性鍵 | 預設值 | 說明 |
|---|---|---|
| `iam.enabled` | `true` | iam 模組總開關。設為 `false` 則不裝配任何 iam Bean（含 Service 與 Controller）。 |
| `iam.api.enabled` | `true` | 是否裝配內建 REST Controller。設為 `false` 可整組關閉，僅保留 Service 供業務程式注入。 |
| `iam.api.base-path` | `/api/iam` | 內建 REST API 的路由前綴。例如預設下帳號端點為 `/api/iam/accounts`。 |
| `iam.group.role-prefix` | `ROLE_` | 群組 `code` 轉為 Spring Security authority 時套用的前綴。權限 `code` 不套前綴、原樣作為 authority。 |
| `iam.ddl.init` | `false` | 是否由 starter 的 `schema-iam.sql` 自動建表。預設關閉，建議交由 Flyway／Liquibase 或手動建表掌控。 |

## application.yml 範例

```yaml
iam:
  enabled: true
  api:
    enabled: true
    base-path: /api/iam
  group:
    role-prefix: ROLE_
  ddl:
    init: false
```

:::tip role-prefix 與 @PreAuthorize 的對應
群組 `code` 為 `ADMIN`、`role-prefix` 為預設 `ROLE_` 時，產生的 authority 為 `ROLE_ADMIN`。此時：

- 以 `hasRole('ADMIN')` 比對（`hasRole` 會自動補上 `ROLE_` 前綴）。
- 權限 `code` 為 `ORDER_EXPORT` 時，authority 即為 `ORDER_EXPORT`，以 `hasAuthority('ORDER_EXPORT')` 比對（不補前綴）。
:::

## 資料表結構

iam 採用五張表（含兩張多對多關聯表），表名與欄位均以 `iam_` 前綴。完整 DDL 位於 starter 的 `src/main/resources/schema-iam.sql`，相容 H2 / MySQL / PostgreSQL。

### iam_account（帳號）

| 欄位 | 型別 | 說明 |
|---|---|---|
| `id` | `BIGINT` PK AUTO_INCREMENT | 主鍵 |
| `username` | `VARCHAR(100)` NOT NULL UNIQUE | 登入帳號，全表唯一 |
| `password` | `VARCHAR(200)` | BCrypt 密碼雜湊；LDAP 來源帳號此欄不使用 |
| `display_name` | `VARCHAR(100)` | 顯示名稱 |
| `enabled` | `BOOLEAN` NOT NULL DEFAULT TRUE | 是否啟用，停用即不可登入 |
| `locked` | `BOOLEAN` NOT NULL DEFAULT FALSE | 是否鎖定 |
| `created_at` | `TIMESTAMP` | 建立時間 |
| `updated_at` | `TIMESTAMP` | 最後更新時間 |

### iam_group（群組／角色）

| 欄位 | 型別 | 說明 |
|---|---|---|
| `id` | `BIGINT` PK AUTO_INCREMENT | 主鍵 |
| `code` | `VARCHAR(100)` NOT NULL UNIQUE | 群組代碼，套 `role-prefix` 後作為 authority |
| `name` | `VARCHAR(100)` | 群組名稱 |
| `description` | `VARCHAR(255)` | 說明 |
| `enabled` | `BOOLEAN` NOT NULL DEFAULT TRUE | 是否啟用 |

### iam_permission（權限）

| 欄位 | 型別 | 說明 |
|---|---|---|
| `id` | `BIGINT` PK AUTO_INCREMENT | 主鍵 |
| `code` | `VARCHAR(100)` NOT NULL UNIQUE | 權限代碼，原樣作為 authority |
| `name` | `VARCHAR(100)` | 權限名稱 |
| `description` | `VARCHAR(255)` | 說明 |
| `enabled` | `BOOLEAN` NOT NULL DEFAULT TRUE | 是否啟用 |

### iam_account_group / iam_group_permission（關聯表）

| 表 | 欄位 | 說明 |
|---|---|---|
| `iam_account_group` | `account_id` + `group_id`（複合 PK） | 帳號↔群組多對多 |
| `iam_group_permission` | `group_id` + `permission_id`（複合 PK） | 群組↔權限多對多 |

:::note 建表時機
`iam.ddl.init` 預設為 `false`，starter 不會自動執行 `schema-iam.sql`。正式環境建議以 Flyway／Liquibase 管理 schema；測試環境可改用 Hibernate `spring.jpa.hibernate.ddl-auto=create-drop` 搭配 H2 自動建表。
:::
