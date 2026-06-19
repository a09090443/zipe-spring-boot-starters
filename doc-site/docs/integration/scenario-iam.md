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

`db-spring-boot-starter` 會自行建立名為 `entityManagerFactory` 的 JPA Bean（以支援多資料來源動態切換），這會使 Spring Boot 預設的 JPA 自動配置退讓。為了讓引入的 starter 不必感知彼此，db-starter 的 `EntityManagerFactory` 在組裝掃描套件時，會**自動併入容器中所有以 `@EntityScan` 註冊的套件**。

iam 的 `IamAutoConfiguration` 已標註 `@EntityScan("com.zipe.entity")`，因此**引入 iam 後無須任何額外設定**——`data-source.properties` 只需照舊列出自身的 Entity 套件即可：

```properties
# entity package 位置（僅需列自身 Entity；iam 等以 @EntityScan 宣告的套件會自動併入）
dynamic.entity-scan=com.example
```

:::info db-starter 4.0.0.1 起自動併入 @EntityScan
db-starter 的 `EntityManagerFactory` 會合併 `dynamic.entity-scan`（仍支援逗號分隔多套件，作為明確指定的選項）與所有 `@EntityScan` 註冊的套件。因此 iam 的 Entity（`com.zipe.entity`）會自動納入管理，其 Repository（`com.zipe.repository`）也自動綁定此 `entityManagerFactory` 與 db-starter 的 `@Primary transactionManager`。
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

:::tip 引入 iam 後，以 `dynamic.base-packages` 重新啟用自身 Repository 掃描
iam 的 `IamAutoConfiguration` 帶有自己的 `@EnableJpaRepositories("com.zipe.repository")`。Spring Boot 的規則是：**一旦容器中出現任何顯式 `@EnableJpaRepositories`，框架對應用主套件的 JPA Repository 自動掃描即退讓**。因此原本僅靠 `@SpringBootApplication` 自動掃描 Repository 的應用，引入 iam 後，自己的 Repository（如 `com.example.repository`）會註冊失敗、啟動報「No qualifying bean of type ...Repository」。

由於本範例同時引入 db-starter，**只需在 `data-source.properties` 設定 `dynamic.base-packages`**，db-starter 即會依此掃描並註冊應用自身的 Repository（等同宣告 `@EnableJpaRepositories`，但改由設定檔驅動、無須改動啟動類別，與 iam 的 `com.zipe.repository` 各自獨立掃描、互不影響）：

```properties
# 應用自身 Repository 套件（可逗號分隔多個）
dynamic.base-packages=com.example.repository
```

故範例的 `Application` 維持單純的 `@SpringBootApplication`，無須再加 `@EnableJpaRepositories`。

> 若未引入 db-starter，則仍以傳統做法在啟動類別宣告 `@EnableJpaRepositories(basePackages = "你的.repository.套件")`。
:::

## 建立資料表與種子資料

範例的 `spring.sql.init.mode` 為 `never`、Hibernate `ddl-auto` 為 `none`，故 iam 的資料表不會自動建立。請於**主資料源**（`example1`）手動套用範例附的 `init/iam-demo.sql`（已含建表與種子資料）：

```bash
mysql --default-character-set=utf8mb4 -u user1 -p example1 < src/main/resources/init/iam-demo.sql
```

:::tip 種子資料含中文，載入時請指定 utf8mb4
`iam-demo.sql` 的 `display_name` 含中文，匯入時務必加上 `--default-character-set=utf8mb4`（或以支援 UTF-8 的客戶端載入），否則 `display_name` 會出現亂碼。
:::

種子資料建立了一條最小授權鏈：

| 帳號 | 群組（角色） | 展開後的 authorities | 用途 |
|---|---|---|---|
| `alice` | `ADMIN` | `ROLE_ADMIN`、`ORDER_EXPORT`、`USER_MANAGE` | 供 `/iam-demo/authorities/{username}` 展示授權解析 |
| `bob` | `USER` | `ROLE_USER`、`ORDER_EXPORT` | 同上 |
| `user01` | `USER` | `ROLE_USER`、`ORDER_EXPORT` | iam 表對帳號 `user01` 的解析結果（`/iam-demo/authorities/user01` 可見） |
| `user02` | `ADMIN` | `ROLE_ADMIN`、`ORDER_EXPORT`、`USER_MANAGE` | iam 表對帳號 `user02` 的解析結果（`/iam-demo/authorities/user02` 可見） |

> 群組 `code` 套用 `iam.group.role-prefix`（預設 `ROLE_`）；權限 `code` 原樣作為 authority。
> `user01`／`user02` 的 `username` 刻意對齊登入帳號。**注意**：範例預設以 `security.basic.users` 登入，登入後帶的是 basic.users 的 authorities（已對齊上表的 `ORDER_EXPORT`／`USER_MANAGE`）；上表是 iam 表自身的群組／權限解析結果，可透過 `/iam-demo/authorities/{username}` 獨立檢視（與登入授權來源無關，見下節）。

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

## 以 iam 權限保護端點

光是「解析得到 authorities」還不夠——要讓權限真正生效，需要兩件事：

**1. 登入後的使用者要帶上 iam 授權。** 範例**預設即為 `custom` 模式**（帳密查 `user_login`：`user01/1234`、`user02/abcd`；`custom-bean-name` 已預先指向 `dbAuthProvider`），登入即可體驗權限端點。custom 模式下 logon 把驗證委派給 `DbAuthProvider`，而 **CUSTOM 模式下框架不會自動套用 `GrantedAuthoritiesResolver`**（僅 BASIC／JWT 的 `IamUserDetailsService`、LDAP 的 `LdapUserDetailsService` 會自動套用）。因此 `DbAuthProvider` 在帳密驗證成功後**自行呼叫 resolver**，把 iam 的群組／權限放進已認證的 token——示範「**認證來源用自家 `user_login`，授權來源用 iam**」的解耦：

```java
@Override
protected UsernamePasswordAuthenticationToken verifyNormalUser(String loginId, String password) {
    UserLogin userLogin = userLoginRepository.findByLoginId(loginId);
    // …帳密比對（略）…
    // 帳密由 user_login 驗證（authn），群組／權限改由 iam 解析（authz）
    var authorities = authoritiesResolver.resolve(loginId);
    return new UsernamePasswordAuthenticationToken(loginId, null, authorities);
}
```

**2. 在端點上宣告所需權限。** logon 的 `SecurityConfiguration` 已標註 `@EnableMethodSecurity(prePostEnabled = true)`，故 `@PreAuthorize` 開箱即用。`IamDemoController` 加了兩個差異化保護的端點：

```java
@GetMapping("/orders/export")
@PreAuthorize("hasAuthority('ORDER_EXPORT')")   // user01、user02 皆可
public String exportOrders() {
    return "訂單已匯出";
}

@GetMapping("/users/manage")
@PreAuthorize("hasAuthority('USER_MANAGE')")     // 僅 user02（ADMIN）
public String manageUsers() {
    return "已進入使用者管理";
}
```

## 驗證端點

啟動後（需主資料源 MySQL 已就緒並套用 `iam-demo.sql`）：

| 功能 | 端點 | 預期結果 |
|---|---|---|
| 解析 alice 的權限 | `GET /example/iam-demo/authorities/alice` | `["ROLE_ADMIN","ORDER_EXPORT","USER_MANAGE"]` |
| 解析 bob 的權限 | `GET /example/iam-demo/authorities/bob` | `["ROLE_USER","ORDER_EXPORT"]` |
| iam 帳號分頁查詢 | `GET /example/iam-demo/accounts` | 含 alice／bob／user01／user02 的分頁 JSON |
| iam 內建帳號 API | `GET /example/api/iam/accounts` | iam 內建 CRUD（受 security 保護，需登入） |

權限保護端點需**先登入**（表單登入 `/example/login`）後再存取。範例預設以 `security.basic.users` 登入，登入者的 authorities **直接來自設定檔**（已刻意對齊下列權限）：

| 登入帳號 | authorities（來自 basic.users） | `GET /example/iam-demo/orders/export`（需 `ORDER_EXPORT`） | `GET /example/iam-demo/users/manage`（需 `USER_MANAGE`） |
|---|---|---|---|
| `user01/1234` | `ORDER_EXPORT` | ✅ `訂單已匯出` | ⛔ HTTP 403 |
| `user02/abcd` | `ORDER_EXPORT`、`USER_MANAGE` | ✅ `訂單已匯出` | ✅ `已進入使用者管理` |
| 未登入 | — | 導向登入頁 | 導向登入頁 |

> 雖然 `/iam-demo/**` 在 `security.allow-uris` 中（URL 層放行），但 `@PreAuthorize` 是**方法層**授權、獨立於 URL 規則之外，因此仍會依登入者的 authorities 把關。

> iam 表的「帳號 → 群組 → 權限」解析仍可透過 `GET /example/iam-demo/authorities/{username}` 獨立展示——該端點直接呼叫 `DbGrantedAuthoritiesResolver`，與登入後的授權來源（此處為 basic.users）無關。

## 切換登入帳號來源（選用）

範例預設以 **basic + `security.basic.users`** 登入：帳密與授權皆來自設定檔，透過 `com.example.config.BasicUserServiceConfig` 在應用層宣告 logon 的 `BasicUserServiceImpl`、覆寫 iam 的帳號接管而生效。若要改由其他來源驗證登入：

- **改走 iam 帳號表（授權由 `DbGrantedAuthoritiesResolver` 解析）**：移除 `BasicUserServiceConfig`，維持 `verification-type: basic`，iam 的 `IamUserDetailsService` 即接管、改讀 `iam_account`（需在 `iam-demo.sql` 為帳號填入真實 BCrypt 密碼雜湊）。
- **改走 `user_login` 表（CUSTOM）**：設 `verification-type: custom` 並 `custom-bean-name: dbAuthProvider`（`DbAuthProvider` 驗帳密、授權再由 iam resolver 疊上）。

:::note 執行期需求
本情境的執行期與整個 `starters_example` 一致，需主資料源（MySQL `example1`）就緒。程式與設定可獨立以 `mvn -o test-compile` 編譯驗證；完整啟動驗證請備妥 MySQL 並先套用 `iam-demo.sql`。
:::
