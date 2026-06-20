---
id: scenario-db
title: 情境二：多資料來源應用
sidebar_position: 3
description: 使用 base + db Starter 建構動態切換資料來源的應用
---

# 情境二：多資料來源應用

## 情境說明

本情境示範如何建構一個**需要連接多個資料庫的系統**。在企業環境中，常見的需求包括：主從資料庫讀寫分離、不同業務模組各自使用獨立資料庫、或整合既有的多套系統資料庫。`db-spring-boot-starter` 透過動態資料來源機制，讓你可以在執行期間以 `@DS` 注解或程式化方式，透明地切換 Repository 實際連接的資料庫，而無須修改 Repository 程式碼。

## 使用的 Starters

| Starter | 在本情境的角色 |
|---|---|
| `base-spring-boot-starter` | 提供 AES 加解密工具（資料來源密碼加密時使用）等基礎設施 |
| `db-spring-boot-starter` | 提供動態多資料來源、JDBC 封裝、`@DS` 切換 AOP |

## pom.xml 依賴配置

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>base-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>db-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>

    <!-- JDBC 驅動（以 MySQL 為例） -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- P6Spy SQL 監控（非必要，但範例預設使用） -->
    <dependency>
        <groupId>p6spy</groupId>
        <artifactId>p6spy</artifactId>
        <version>3.9.1</version>
    </dependency>
</dependencies>
```

## data-source.properties 設定方式

`db-spring-boot-starter` 從 `data-source.properties` 以 Map 結構動態注入多個資料來源。以下為 `starters_example` 的設定（定義 `example1`、`example2` 兩個 MySQL 資料來源）：

```properties
# 預設主要資料來源名稱
dynamic.primary=example1
# JPA Entity 掃描套件
dynamic.entity-scan=com.example
# 密碼是否以 AES 加密
dynamic.is-encrypt=false

# example1 資料來源
dynamic.data-source-map[example1].name=example1
dynamic.data-source-map[example1].url=jdbc:p6spy:mysql://localhost:3306/example1?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC
dynamic.data-source-map[example1].username=user1
dynamic.data-source-map[example1].pa55word=example1
dynamic.data-source-map[example1].driverClassName=com.p6spy.engine.spy.P6SpyDriver

# example2 資料來源
dynamic.data-source-map[example2].name=example2
dynamic.data-source-map[example2].url=jdbc:p6spy:mysql://localhost:3306/example2?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC
dynamic.data-source-map[example2].username=user2
dynamic.data-source-map[example2].pa55word=example2
dynamic.data-source-map[example2].driverClassName=com.p6spy.engine.spy.P6SpyDriver
```

:::note 為什麼密碼欄位叫 pa55word？
密碼欄位鍵名刻意使用 `pa55word`（而非 `password`），是為了避免某些框架自動掃描與遮蔽密碼欄位。所有資料來源統一以 **P6Spy** 包裝 Driver（`jdbc:p6spy:mysql://...`），用途是攔截並記錄所有實際執行的 SQL，方便除錯。若不需要 P6Spy，`driverClassName` 直接填 `com.mysql.cj.jdbc.Driver` 並移除 URL 中的 `p6spy:` 前綴即可。
:::

## 資料模型設計

`starters_example` 定義了兩個 JPA Entity，分別對應 `user_main` 與 `user_detail` 兩張表。

```java
@Entity
@Getter
@Setter
@Table(name = "user_main")
public class UserMain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
}
```

```java
@Entity
@Getter
@Setter
@Table(name = "user_detail")
public class UserDetail {

    @Id
    private String name;   // 主鍵：對應 UserMain.name

    private String gender;
}
```

`UserDetail.name` 在語意上是 `UserMain.name` 的外鍵（邏輯關聯），但程式碼層面**並未使用 `@OneToOne` 或 `@JoinColumn` 宣告 JPA 關聯**，屬於手動關聯設計。

## ExampleJdbc 使用方式

除了 JPA Repository，`db-spring-boot-starter` 還提供 `BaseJDBC` 基底類別。`ExampleJdbc` 本身是空類別，所有功能繼承自 `BaseJDBC`：

```java
@Repository
public class ExampleJdbc extends BaseJDBC {
}
```

`BaseJDBC` 的設計重點：

- SQL 不寫在程式碼中，而是從 classpath 或外部檔案系統讀取 `.sql` 檔案，透過 `ResourceEnum` 枚舉指向檔案路徑。
- 提供 SQL 快取（`ConcurrentHashMap`），同一個 `ResourceEnum` 只讀取一次檔案。
- 分頁使用 `Paging` 物件，SQL 範本中以 `${START}`、`${ENDED}`、`${ORDER_BY}`、`${QUERY_STRING}` 佔位符動態替換。

| 方法 | 用途 |
|---|---|
| `queryForBean(resource, clazz)` | 查詢單一物件 |
| `queryForMap(resource)` | 查詢單列 Map |
| `queryForList(resource, clazz)` | 查詢清單（可加 `Conditions`、`Paging`） |
| `queryForObject(resource, params, clazz)` | 查詢純量值 |
| `update(resource, params)` | 更新/新增/刪除 |
| `updateBatch(resource, params)` | 批次更新 |

## Repository 如何指定資料來源

兩個 Repository 皆繼承 `JpaRepository`，且**完全不需要知道要用哪個資料來源**：

```java
public interface UserMainRepository extends JpaRepository<UserMain, Integer> {
    UserMain findUserByName(@Param("name") String name);
}

public interface UserDetailRepository extends JpaRepository<UserDetail, String> {
    UserDetail findByName(String name);
    UserDetail findByGender(String gender);
}
```

實際使用哪個資料來源，由 `DynamicDataSource`（繼承 `AbstractRoutingDataSource`）在執行時透過 `ThreadLocal` 決定。呼叫端在查詢前設定好 `DataSourceHolder` 即可透明切換。

## DBExampleServiceImpl 的資料來源切換範例

`starters_example` 的 `DBExampleServiceImpl` 示範了兩種切換方式：

### 方式一：程式化手動切換

```java
@Override
public UserMain getUserMainByName(String name) {
    DataSourceHolder.getDataSourceName();            // 讀取目前 DS
    DataSourceHolder.setDataSourceName("example2");  // 手動切換至 example2
    return userMainRepository.findUserByName(name);
}
```

適合需要在同一方法中切換多次的場景。切換目標須為 `data-source.properties` 中**實際存在**的資料源名稱；若指定不存在的名稱，`DynamicDataSource` 會 fallback 回 primary，導致「看似切換、實則未切換」。

### 方式二：@DS 注解宣告式切換

```java
@Override
@DS("example2")   // 由 AOP 自動切換至 example2，方法結束後還原
public UserDetail getUserDetailByName(String name) {
    return userDetailRepository.findByName(name);
}
```

`@DS` 的 `value` 預設為 `"common"`，實務上應明確指定為設定中存在的資料源名稱（如 `@DS("example2")`）。AOP 切面 `DynamicDataSourceAspect` 標記 `@Order(-1)`，確保在 `@Transactional` 之前執行（交易開始前就要確定資料來源）。

### 方式三：以參數動態指定資料來源

除了前兩種「切換目標寫死在程式碼」的方式，`DBExampleService` 另提供以參數傳入資料來源名稱的多載，讓呼叫端在執行期決定要查哪個資料源：

```java
@Override
public UserMain getUserMainByName(String name, String dataSourceName) {
    try {
        DataSourceHolder.setDataSourceName(dataSourceName);  // 切換至指定資料源
        return userMainRepository.findUserByName(name);
    } finally {
        DataSourceHolder.clearDataSourceName();              // 查詢後清除 ThreadLocal
    }
}
```

以 `try/finally` 包覆，確保查詢結束後一定清除 `DataSourceHolder`，避免執行緒池重用時殘留錯誤的資料來源名稱。

## 透過 REST API 實測切換（DbExampleController）

`starters_example` 提供 `DbExampleController`，將上述「以參數指定資料來源」的能力開放為 HTTP 端點，可直接用瀏覽器或 `curl` 觀察切換效果：

```java
@RestController
@RequestMapping("/rest/db")
public class DbExampleController {

    @GetMapping("/user")
    public ResponseEntity<UserMain> getUser(
            @RequestParam String name,
            @RequestParam(name = "ds", defaultValue = "example1") String dataSourceName) {
        UserMain userMain = dbExampleService.getUserMainByName(name, dataSourceName);
        return ResponseEntity.ofNullable(userMain);
    }
}
```

| 參數 | 說明 | 預設值 |
|---|---|---|
| `name` | 欲查詢的使用者名稱 | （必填） |
| `ds` | 目標資料來源名稱（須為 `data-source.properties` 實際存在的資料源） | `example1` |

搭配各資料源的**獨有標記資料**即可驗證切換是否真正生效——查得資料回傳 `200`，查無資料回傳 `404`：

```bash
# 切到 example1（MySQL）查 example1 獨有資料 → 200，查得到
curl "http://localhost:8080/example/rest/db/user?name=OnlyExample1&ds=example1"

# 切到 example2 查 example1 的獨有資料 → 404，查不到（證明確實路由到 example2）
curl "http://localhost:8080/example/rest/db/user?name=OnlyExample1&ds=example2"

# 切到 PostgreSQL 查其獨有資料 → 200
curl "http://localhost:8080/example/rest/db/user?name=OnlyPostgres&ds=postgres"
```

也可直接在**瀏覽器**貼上上述網址觀察結果（200 顯示 JSON、404 顯示空白／錯誤頁）。

:::note 為什麼這支端點免登入？
範例採用 logon-starter 的**表單登入**模式（`anyRequest().authenticated()`），未認證的請求會被導向 `/login` 登入頁。為方便用瀏覽器直接示範切換效果，本範例已把 `/rest/db/**` 加入 `security.allow-uris` 放行（與 `/iam-demo/**` 等示範端點同樣做法）。

若改成**保留認證**來測試，移除 `allow-uris` 中的 `/rest/db/**` 後，可：
- **瀏覽器**：先到 `http://localhost:8080/example/login` 以 `user01/1234`（或 `user02/abcd`）登入，再貼上 API 網址；或
- **curl**：該 chain 同時啟用 HTTP Basic，可帶 `-u user01:1234` 存取。

帳密由 `security.basic.users` 提供（本範例為 `user01/1234`、`user02/abcd`），非 `admin/admin`。
:::

:::warning ThreadLocal 切換的注意事項
資料來源切換是以 `ThreadLocal`（`DataSourceHolder`）實作，因此有以下幾點需特別留意：

- **執行緒邊界**：`ThreadLocal` 只在當前執行緒有效。若在方法中啟動新執行緒（或使用 `@Async`、執行緒池），子執行緒不會繼承父執行緒的資料來源設定，必須重新呼叫 `DataSourceHolder.setDataSourceName(...)`。
- **及時清除**：使用程式化切換時，務必在使用完畢後清除（或在切面結束時自動清除），避免執行緒被池化重用時殘留錯誤的資料來源名稱。`@DS` 注解方式由 AOP 自動處理清除，較為安全。
- **與交易的順序**：`@DS` 必須在 `@Transactional` 之前生效，因此切面 `@Order(-1)` 不可隨意調整。在同一個交易內切換資料來源並不會生效（交易已綁定特定連線）。
:::

## 跨資料庫類型動態切換（MySQL ↔ PostgreSQL）

動態資料來源不限於同類型資料庫，也能在**不同資料庫產品**之間切換。`starters_example` 以三個資料來源示範：

| 資料源 key | 類型 | 連線 | 獨有標記資料 |
|---|---|---|---|
| `example1`（primary） | MySQL | `localhost:3306/example1` | `OnlyExample1` |
| `example2` | MySQL | `localhost:3306/example2` | `OnlyExample2` |
| `postgres` | PostgreSQL | `localhost:5432/pgdb` | `OnlyPostgres` |

三個資料來源具有相同的基礎資料（Tom/Jen/Andy/Gary），各自再放一筆**獨有標記**，
用以辨識查詢究竟被路由到哪個資料來源——這是驗證「切換確實生效、而非 fallback 回 primary」的關鍵。

### 測試資料初始化

整合測試所需的資料庫、帳號、資料表與測試資料，以初始化腳本維護於：

```
starters_example/src/test/resources/db/
├── mysql-init.sql      # example1 / example2（建庫、帳號、資料表、資料）
├── postgres-init.sql   # pgdb（資料表、資料）
├── apply.sh / apply.ps1 # 一鍵套用
└── README.md           # 資料源對照與套用說明
```

啟動 Docker 資料庫後執行 `apply.sh`（或 `apply.ps1`）即可重建完整測試環境，腳本為冪等設計，可重複執行重置。

### 驗證測試

本範例提供兩個互補的切換驗證測試：

**`DynamicDataSourceSwitchTest`** — 驗證同類型（MySQL `example1` ↔ `example2`）切換：

```java
// 切換至 example1：只查得到 example1 的獨有資料
DataSourceHolder.setDataSourceName("example1");
assertNotNull(userMainRepository.findUserByName("OnlyExample1"));
assertNull(userMainRepository.findUserByName("OnlyExample2"));

// 切換至 example2：只查得到 example2 的獨有資料
DataSourceHolder.setDataSourceName("example2");
assertNotNull(userMainRepository.findUserByName("OnlyExample2"));
assertNull(userMainRepository.findUserByName("OnlyExample1"));
```

**`CrossDbSwitchTest`** — 以「互斥資料法」證明跨類型切換生效：

```java
// 切換至 example1（MySQL）：只查得到 MySQL 的獨有資料
DataSourceHolder.setDataSourceName("example1");
assertNotNull(userMainRepository.findUserByName("OnlyExample1"));  // MySQL 有
assertNull(userMainRepository.findUserByName("OnlyPostgres"));     // MySQL 無

// 切換至 postgres（PostgreSQL）：只查得到 PostgreSQL 的獨有資料
DataSourceHolder.setDataSourceName("postgres");
assertNotNull(userMainRepository.findUserByName("OnlyPostgres"));  // PostgreSQL 有
assertNull(userMainRepository.findUserByName("OnlyExample1"));     // PostgreSQL 無
```

四個斷言僅在查詢「真正跨資料庫類型路由」時才能同時成立——若切換失效而 fallback 至同一資料來源，`assertNull` 必然失敗。

PostgreSQL driver 需由引用方自行加入（`org.postgresql:postgresql`），跨方言的限制請參閱
[db-starter 設定文件的「跨資料庫類型的方言限制」](../db-starter/configuration.md#混用不同資料庫類型mysql--postgresql)。
