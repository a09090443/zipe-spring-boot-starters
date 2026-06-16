---
id: quickstart
title: 快速開始
sidebar_position: 2
description: 從引入依賴到以資料庫帳號登入並以權限保護端點的最短路徑
---

# 快速開始

本頁示範如何在數步內讓業務系統具備「資料庫帳號登入 + 群組／權限授權」能力。

## 1. 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>iam-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

`iam-spring-boot-starter` 已相依 `logon-spring-boot-starter` 與 `spring-boot-starter-data-jpa`，無須額外宣告 Spring Security 與 JPA。請於專案根目錄先執行 `mvn clean install` 安裝模組至本地 Repository。

## 2. 設定資料來源與建表

iam 以 JPA 持久化，需提供資料來源。以下以 H2 為例（正式環境請改為 MySQL／PostgreSQL）：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:iam;DB_CLOSE_DELAY=-1
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: none

iam:
  ddl:
    init: false
```

建表方式擇一：

- **正式環境（建議）**：以 Flyway／Liquibase 套用 starter 的 `schema-iam.sql`（五張 `iam_` 表）。
- **快速試用**：將 `spring.jpa.hibernate.ddl-auto` 設為 `update` 或 `create`，由 Hibernate 依實體自動建表。

## 3. 建立權限、群組與帳號

啟動後即可透過內建 REST API（預設啟用，前綴 `/api/iam`）建立資料。以下用 `curl` 示範一條最小授權鏈：

```bash
# 建立一個權限（泛用具名授權點）
curl -X POST http://localhost:8080/api/iam/permissions \
  -H 'Content-Type: application/json' \
  -d '{"code":"ORDER_EXPORT","name":"匯出訂單","description":"允許匯出訂單報表"}'

# 建立一個群組（角色），假設回傳 id=1
curl -X POST http://localhost:8080/api/iam/groups \
  -H 'Content-Type: application/json' \
  -d '{"code":"ADMIN","name":"系統管理員"}'

# 將權限（id=1）掛到群組（id=1）
curl -X POST http://localhost:8080/api/iam/groups/1/permissions/1

# 建立帳號（密碼為明文，服務層會 BCrypt 編碼），假設回傳 id=1
curl -X POST http://localhost:8080/api/iam/accounts \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"secret","displayName":"Alice"}'

# 將帳號（id=1）加入群組（id=1）
curl -X POST http://localhost:8080/api/iam/accounts/1/groups/1
```

此時帳號 `alice` 將擁有 authorities：`ROLE_ADMIN`（群組 code 套 `role-prefix`）與 `ORDER_EXPORT`（權限 code 原樣）。

## 4. 以資料庫帳號登入

iam 自動接上 logon 的登入流程，**不需額外設定**：

- **BASIC／JWT 模式**：`IamUserDetailsService` 已取代 logon 內建的 `admin/admin` stub，登入帳號改由 `iam_account` 表提供。以 `alice / secret` 即可登入。
- **LDAP 模式**：帳號密碼仍由 LDAP 驗證，但登入成功後的 authorities 由 iam 群組／權限提供（需在 `iam_account` 建立同名帳號並指派群組）。

## 5. 以權限保護端點

在業務應用啟用方法層級安全，並以標準註解比對 iam 產生的 authorities：

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
```

```java
@RestController
public class OrderController {

    @PreAuthorize("hasRole('ADMIN')")          // 對應群組 code = ADMIN
    @GetMapping("/orders")
    public List<Order> list() { ... }

    @PreAuthorize("hasAuthority('ORDER_EXPORT')") // 對應權限 code = ORDER_EXPORT
    @GetMapping("/orders/export")
    public Resource export() { ... }
}
```

至此，業務系統已具備資料庫帳號登入與群組／權限授權能力。進一步的客製化（覆寫帳號來源、調整授權規則）請見[使用範例](./examples.md)。
