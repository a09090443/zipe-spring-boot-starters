---
id: scenario-iam
title: 情境五：身分與授權管理
sidebar_position: 6
description: 使用 db + logon + iam Starter 建構資料庫帳號／群組／權限的身分授權系統
---

# 情境五：身分與授權管理

## 情境說明

本情境示範如何讓系統具備**資料庫帳號管理與權限控管**能力。`iam-spring-boot-starter` 提供帳號（Account）—群組（Group）—權限（Permission）三層模型並以 JPA 持久化，再透過 logon 的 `GrantedAuthoritiesResolver` 擴充點，把使用者的 Spring Security authorities 直接由資料庫的群組與權限展開——三種驗證模式（BASIC／LDAP／CUSTOM）皆共用同一套授權來源。

`starters_example` 將 iam 與既有的 db、logon 一起裝配，重點在於 iam 的 Entity 如何與 db-starter 的動態資料來源**共用同一個 `EntityManagerFactory`**。

## 使用的 Starters

| Starter | 在本情境的角色 |
|---|---|
| `db-spring-boot-starter` | 提供動態資料來源與 `EntityManagerFactory`，並一併管理 iam 的 Entity |
| `logon-spring-boot-starter` | 提供登入認證與 `GrantedAuthoritiesResolver` SPI |
| `iam-spring-boot-starter` | 提供帳號／群組／權限模型、Service、內建 REST API 與 `DbGrantedAuthoritiesResolver` |

## 關鍵整合點：與 db-starter 共用 EntityManagerFactory

這是本情境**最重要**的設定。`db-spring-boot-starter` 會自行建立名為 `entityManagerFactory` 的 JPA Bean（以支援多資料來源動態切換），這會使 Spring Boot 預設的 JPA 自動配置退讓。因此 iam 的 `@EntityScan("com.zipe.entity")` 不會生效，必須改由 db-starter 的 `entityManagerFactory` 一併掃描 iam 的 Entity 套件。

做法：在 `data-source.properties` 的 `dynamic.entity-scan` 以**逗號分隔**加入 `com.zipe.entity`：

```properties
# entity package 位置（逗號分隔可指定多個套件；含 iam-starter 的 com.zipe.entity）
dynamic.entity-scan=com.example,com.zipe.entity
```

:::info db-starter 4.0.0.1 起支援多套件
`dynamic.entity-scan` 自此版本起支援逗號分隔的多個套件，正是為了讓 db-starter 的 `EntityManagerFactory` 能同時管理業務模組與 iam 的 Entity。iam 的 Repository（`com.zipe.repository`）會自動綁定此 `entityManagerFactory` 與 db-starter 的 `@Primary transactionManager`。
:::

## 引入依賴

於 `starters_example/pom.xml` 在 logon 之後加入 iam：

```xml
<dependency>
  <groupId>io.github.a09090443</groupId>
  <artifactId>logon-spring-boot-starter</artifactId>
  <version>${zipe.spring.starter.version}</version>
</dependency>
<dependency>
  <groupId>io.github.a09090443</groupId>
  <artifactId>iam-spring-boot-starter</artifactId>
  <version>${zipe.spring.starter.version}</version>
</dependency>
```

## 設定

`iam.*` 全部採用預設值即可（總開關啟用、內建 API 啟用、群組前綴 `ROLE_`），故 `application.yml` **無須新增 iam 區塊**。僅需在 `security.allow-uris` 放行示範端點：

```yaml
security:
  # …
  allow-uris: /resources/**,/static/**,/webservice/**,/iam-demo/** # 放行 iam 整合示範端點
```

## 建立資料表與種子資料

範例的 `spring.sql.init.mode` 為 `never`、Hibernate `ddl-auto` 為 `none`，故 iam 的資料表不會自動建立。請於**主資料源**（`example1`）手動套用範例附的 `init/iam-demo.sql`（已含建表與種子資料）：

```bash
mysql -u user1 -p example1 < src/main/resources/init/iam-demo.sql
```

種子資料建立了一條最小授權鏈：

| 帳號 | 群組（角色） | 展開後的 authorities |
|---|---|---|
| `alice` | `ADMIN` | `ROLE_ADMIN`、`ORDER_EXPORT`、`USER_MANAGE` |
| `bob` | `USER` | `ROLE_USER`、`ORDER_EXPORT` |

> 群組 `code` 套用 `iam.group.role-prefix`（預設 `ROLE_`）；權限 `code` 原樣作為 authority。

## 示範程式

`com.example.controller.IamDemoController` 注入 iam 的 `AccountService` 與被 iam 覆寫的 `GrantedAuthoritiesResolver`，示範內建 API 看不到的「授權解析結果」：

```java
@RestController
@RequestMapping("/iam-demo")
public class IamDemoController {

    private final AccountService accountService;
    private final GrantedAuthoritiesResolver authoritiesResolver;

    public IamDemoController(AccountService accountService,
                             GrantedAuthoritiesResolver authoritiesResolver) {
        this.accountService = accountService;
        this.authoritiesResolver = authoritiesResolver;
    }

    @GetMapping("/accounts")
    public Page<AccountVO> accounts(Pageable pageable) {
        return accountService.listAccounts(pageable);
    }

    @GetMapping("/authorities/{username}")
    public List<String> authorities(@PathVariable String username) {
        return authoritiesResolver.resolve(username).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
```

## 驗證端點

啟動後（需主資料源 MySQL 已就緒並套用 `iam-demo.sql`）：

| 功能 | 端點 | 預期結果 |
|---|---|---|
| 解析 alice 的權限 | `GET /example/iam-demo/authorities/alice` | `["ROLE_ADMIN","ORDER_EXPORT","USER_MANAGE"]` |
| 解析 bob 的權限 | `GET /example/iam-demo/authorities/bob` | `["ROLE_USER","ORDER_EXPORT"]` |
| iam 帳號分頁查詢 | `GET /example/iam-demo/accounts` | 含 alice／bob 的分頁 JSON |
| iam 內建帳號 API | `GET /example/api/iam/accounts` | iam 內建 CRUD（受 security 保護，需登入） |

## 以 iam 帳號表登入（選用）

範例預設為 `custom` 登入模式（`DbAuthProvider` 走 `user_login` 表）。若要改由 **iam 帳號表**驗證登入，將 `security.verification-type` 改為 `basic`：此時 iam 的 `IamUserDetailsService` 會自動取代 logon 內建的 `admin/admin` stub，登入帳號改讀 `iam_account`（需在 `iam-demo.sql` 為帳號填入真實 BCrypt 密碼雜湊）。授權部分無論哪種模式都由 `DbGrantedAuthoritiesResolver` 提供。

:::note 執行期需求
本情境的執行期與整個 `starters_example` 一致，需主資料源（MySQL `example1`）就緒。程式與設定可獨立以 `mvn -o test-compile` 編譯驗證；完整啟動驗證請備妥 MySQL 並先套用 `iam-demo.sql`。
:::
