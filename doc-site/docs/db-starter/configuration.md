---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `db-spring-boot-starter` 的所有可設定屬性。設定主要集中於 `dynamic.*`（對應 `data-source.properties`），用於宣告主資料來源與多組命名資料來源。

:::warning 屬性前綴

本 Starter 的設定前綴為 `dynamic`，**非** `spring.datasource`、`zipe.datasource` 或其他前綴。所有屬性都需放在 `src/main/resources/data-source.properties`（引用方自建，覆蓋 Starter 內部的預設範本）。

:::

## 頂層屬性

| 屬性鍵 | 型別 | 預設值 | 必填 | 說明 |
|---|---|---|---|---|
| `dynamic.primary` | String | 無 | 是 | 預設使用的資料來源 key 名稱；須對應 `data-source-map` 中的某個 key |
| `dynamic.base-packages` | String | 無 | 否 | 需掃描並註冊 Spring Data JPA Repository 的套件路徑，可逗號分隔多個。等同宣告 `@EnableJpaRepositories(basePackages = ...)`，但改由設定檔驅動。**僅在引入自帶 `@EnableJpaRepositories` 的模組（如 iam-starter）後需要**，用以重新啟用應用自身 Repository 的掃描；未設定時維持 Spring Boot 預設行為。詳見下方〈以設定檔啟用 JPA Repository 掃描〉 |
| `dynamic.entity-scan` | String | 無 | 是 | JPA Entity 掃描套件路徑，例如 `com.example`；可逗號分隔指定多個套件。另外，所有以 `@EntityScan` 註冊的套件（如併用 iam-starter 的 `com.zipe.entity`）會自動併入，無須在此重複設定 |
| `dynamic.is-encrypt` | Boolean | `false` | 否 | 密碼（`pa55word`）是否經 Base64 編碼；設為 `true` 時模組自動以 Base64 解碼 |
| `dynamic.data-source-map` | Map | 無 | 是 | 所有命名資料來源的設定集合 |

## 單一資料來源屬性

`dynamic.data-source-map[<key>].*` 之下可設定的屬性如下，`<key>` 為自訂的資料來源名稱（即 `@DS` 中使用的字串）：

| 屬性鍵（後綴） | 型別 | 預設值 | 必填 | 說明 |
|---|---|---|---|---|
| `.url` | String | 無 | 是 | JDBC 連線字串；使用 P6Spy 時需加 `p6spy:` 前綴，例如 `jdbc:p6spy:mysql://...` |
| `.username` | String | 無 | 是 | 資料庫帳號 |
| `.pa55word` | String | 無 | 是 | 資料庫密碼（**欄位名稱為 `pa55word`，非 `password`**） |
| `.driverClassName` | String | 無 | 是 | JDBC 驅動類別；使用 P6Spy 時填 `com.p6spy.engine.spy.P6SpyDriver` |
| `.name` | String | 無 | 否 | 資料來源顯示名稱（僅供識別用） |

:::danger pa55word 欄位名稱

密碼欄位名稱為 `pa55word`（數字 `5` 取代字母 `s`），這是刻意設計以避開自動安全掃描工具。若誤寫為 `password`，屬性綁定會靜默失敗，密碼為空字串，連線時拋出認證錯誤。

:::

## 以設定檔啟用 JPA Repository 掃描（`dynamic.base-packages`）

db-starter 自建 `entityManagerFactory`，會使 Spring Boot 的 JPA 自動配置退讓；單獨使用 db-starter 時，Spring Boot 仍會自動掃描應用主套件下的 Repository，無須額外設定。

但若再引入**自帶 `@EnableJpaRepositories` 的模組**（例如 iam-starter 帶有 `@EnableJpaRepositories("com.zipe.repository")`），依 Spring Boot 規則，**容器中只要出現任何顯式 `@EnableJpaRepositories`，框架對應用主套件的 Repository 自動掃描即退讓**，導致應用自身的 Repository 註冊失敗（啟動時報 `No qualifying bean of type ...Repository`）。

此時只要在 `data-source.properties` 設定 `dynamic.base-packages`，db-starter 的 `DynamicJpaRepositoriesRegistrar` 便會依此掃描並註冊對應套件下的 Repository，效果等同於宣告 `@EnableJpaRepositories`，但改由設定檔驅動、無須改動啟動類別：

```properties
# 應用自身 Repository 套件（可逗號分隔多個），與 iam 的 com.zipe.repository 各自獨立掃描、互不影響
dynamic.base-packages=com.example.repository
```

未設定 `dynamic.base-packages` 時，此機制不介入，維持 Spring Boot 既有掃描行為（向後相容）。

## HikariCP 連線池設定

HikariCP 的連線池參數**目前寫死於 `BaseDataSourceConfig.baseHikariConfig()`**，無法透過 `data-source.properties` 覆蓋。如需調整，須修改 Starter 原始碼後重新建構。

| 設定 | 預設值 | 說明 |
|---|---|---|
| `minimumIdle` | `5` | 最小閒置連線數 |
| `maximumPoolSize` | `20` | 最大連線數 |
| `idleTimeout` | `30,000` ms | 閒置連線逾時時間 |
| `maxLifetime` | `2,000,000` ms（約 33 分鐘） | 連線最長生命週期 |
| `connectionTimeout` | `30,000` ms | 等待連線逾時時間 |
| `connectionTestQuery` | `SELECT 1` | 連線健康檢查語句（AS400 自動覆蓋為 `VALUES 1`） |
| `cachePrepStmts` | `true` | 預備語句快取 |
| `prepStmtCacheSize` | `250` | 預備語句快取上限 |
| `prepStmtCacheSqlLimit` | `2,048` | 單條語句最大長度 |
| `useServerPrepStmts` | `true` | 使用 Server-side Prepared Statements |

## P6Spy 監控設定

P6Spy **不透過 Spring 屬性設定**，而是透過靜態的 `spy.properties` 檔案（位於 Starter 的 classpath）。引用方若需自訂，可在自己的 `src/main/resources/spy.properties` 中覆蓋。

主要預設值：

| spy.properties 鍵 | 預設值 | 說明 |
|---|---|---|
| `appender` | `com.p6spy.engine.spy.appender.Slf4JLogger` | 日誌輸出器，使用 SLF4J |
| `driverlist` | SQL Server / AS400 / MySQL | 已知驅動清單（新增其他資料庫時需在 `driverlist` 補上） |
| `outagedetection` | `true` | 啟用慢查詢偵測 |
| `outagedetectioninterval` | `2`（秒） | 超過此時間的 SQL 被標記為慢查詢 |
| `excludecategories` | `info,debug,result,batc,resultset` | 排除非 SQL 執行的日誌類型 |

**啟用 P6Spy：** 將 JDBC URL 的 `jdbc:mysql://` 改為 `jdbc:p6spy:mysql://`，驅動改為 `com.p6spy.engine.spy.P6SpyDriver`。若不需要 P6Spy，可直接使用原生驅動與 URL，功能不受影響。

## 完整 data-source.properties 範例

### 雙資料來源（MySQL + MySQL）

```properties
# 頂層設定
dynamic.primary=master
dynamic.entity-scan=com.example
dynamic.is-encrypt=false

# 主資料來源
dynamic.data-source-map[master].url=jdbc:p6spy:mysql://localhost:3306/main_db?characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Taipei
dynamic.data-source-map[master].username=root
dynamic.data-source-map[master].pa55word=master_password
dynamic.data-source-map[master].driverClassName=com.p6spy.engine.spy.P6SpyDriver

# 報表資料來源
dynamic.data-source-map[report].url=jdbc:p6spy:mysql://localhost:3306/report_db?characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Taipei
dynamic.data-source-map[report].username=reader
dynamic.data-source-map[report].pa55word=reader_password
dynamic.data-source-map[report].driverClassName=com.p6spy.engine.spy.P6SpyDriver
```

### 混用多種資料庫（MySQL + SQL Server）

```properties
dynamic.primary=mysql_db
dynamic.entity-scan=com.example
dynamic.is-encrypt=false

# MySQL 資料來源
dynamic.data-source-map[mysql_db].url=jdbc:p6spy:mysql://localhost:3306/mydb?characterEncoding=UTF-8&serverTimezone=Asia/Taipei
dynamic.data-source-map[mysql_db].username=user
dynamic.data-source-map[mysql_db].pa55word=password
dynamic.data-source-map[mysql_db].driverClassName=com.p6spy.engine.spy.P6SpyDriver

# SQL Server 資料來源
dynamic.data-source-map[mssql_db].url=jdbc:p6spy:sqlserver://localhost:1433;databaseName=mydb
dynamic.data-source-map[mssql_db].username=sa
dynamic.data-source-map[mssql_db].pa55word=SqlPassword1
dynamic.data-source-map[mssql_db].driverClassName=com.p6spy.engine.spy.P6SpyDriver
```

### 混用不同資料庫類型（MySQL + PostgreSQL）

動態資料來源可在**不同資料庫產品之間**切換。以下示範 primary 為 MySQL、另一個資料來源為 PostgreSQL：

```properties
dynamic.primary=example1
dynamic.entity-scan=com.example
dynamic.is-encrypt=false

# MySQL 資料來源
dynamic.data-source-map[example1].url=jdbc:p6spy:mysql://localhost:3306/example1?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC
dynamic.data-source-map[example1].username=user1
dynamic.data-source-map[example1].pa55word=example1
dynamic.data-source-map[example1].driverClassName=com.p6spy.engine.spy.P6SpyDriver

# PostgreSQL 資料來源
dynamic.data-source-map[postgres].url=jdbc:p6spy:postgresql://localhost:5432/pgdb
dynamic.data-source-map[postgres].username=pguser
dynamic.data-source-map[postgres].pa55word=pgpass
dynamic.data-source-map[postgres].driverClassName=com.p6spy.engine.spy.P6SpyDriver
```

PostgreSQL driver 並未隨 Starter 提供，引用方需自行加入依賴（版本可交由 Spring Boot BOM 管理）：

```xml
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <scope>runtime</scope>
</dependency>
```

:::warning 跨資料庫類型的方言限制

多個資料來源共用**單一** `EntityManagerFactory`，Hibernate 方言（dialect）以 `primary` 資料來源偵測。基本 CRUD 查詢（`SELECT / INSERT / UPDATE / DELETE`）的 SQL 在不同資料庫間語法通用，可正常跨類型切換；但若用到**方言專屬語法**（分頁 `LIMIT` vs `OFFSET FETCH`、資料庫專屬函式、型別轉換等），以 primary 方言產生的 SQL 可能不適用於另一類型的資料庫。此情境下建議為各資料庫類型配置獨立的 `EntityManagerFactory`。

:::

### 啟用密碼加密

當 `dynamic.is-encrypt=true` 時，模組會以 `CryptoUtil(Base64Util)` 對 `pa55word` 進行 Base64 解碼：

```properties
dynamic.primary=master
dynamic.entity-scan=com.example
dynamic.is-encrypt=true

dynamic.data-source-map[master].url=jdbc:p6spy:mysql://localhost:3306/mydb?serverTimezone=Asia/Taipei
dynamic.data-source-map[master].username=root
# pa55word 填入 Base64 編碼後的密文
dynamic.data-source-map[master].pa55word=Base64EncodedPasswordHere==
dynamic.data-source-map[master].driverClassName=com.p6spy.engine.spy.P6SpyDriver
```

### AS400 資料來源

AS400 使用特殊的連線建立邏輯，只要 URL 中包含 `as400`（大小寫不限），模組會自動走 AS400 分支（`connectionTestQuery` 改為 `VALUES 1`，不套用 MySQL 優化設定）：

```properties
dynamic.primary=common
dynamic.entity-scan=com.example
dynamic.is-encrypt=false

dynamic.data-source-map[common].url=jdbc:p6spy:as400://192.168.1.100/mylib
dynamic.data-source-map[common].username=user400
dynamic.data-source-map[common].pa55word=pass400
dynamic.data-source-map[common].driverClassName=com.p6spy.engine.spy.P6SpyDriver
```

## 分頁 SQL 模板（使用 Paging 功能時必填）

若要使用 `Paging` 進行伺服器端分頁，**必須**在引用方的 `src/main/resources/sql/PAGING.sql` 提供分頁 SQL 模板。模板需包含以下佔位符：

| 佔位符 | 說明 |
|---|---|
| `${QUERY_STRING}` | 原始查詢 SQL |
| `${ORDER_BY}` | 排序欄位（`Paging.orderBy` 合併後的字串） |
| `${START}` | 起始列號（`(page-1)*pagesize+1`） |
| `${ENDED}` | 結束列號（`page*pagesize`） |

**SQL Server 分頁模板範例：**

```sql
SELECT * FROM (
    SELECT ROW_NUMBER() OVER (ORDER BY ${ORDER_BY}) AS ROW_NUM, T.* FROM (
        ${QUERY_STRING}
    ) T
) PAGED_RESULT
WHERE ROW_NUM BETWEEN ${START} AND ${ENDED}
```

**MySQL 分頁模板範例（使用 LIMIT OFFSET）：**

```sql
SELECT * FROM (
    ${QUERY_STRING}
    ORDER BY ${ORDER_BY}
) T
LIMIT ${ENDED}
OFFSET ${START}
```

:::warning 缺少 PAGING.sql 的後果

若 classpath 中沒有 `/sql/PAGING.sql`，Paging 的靜態初始化區塊會靜默失敗（`pageingTemplate` 為 `null`），待執行分頁查詢時拋出 `NullPointerException`，且堆疊追蹤不易定位原因。此問題無啟動期驗證，請務必預先放置正確的分頁模板。

:::

## 手動操作 DataSourceHolder

若需要在不使用 `@DS` 的情況下手動切換資料來源（例如在非 Spring Bean 的工具類別中），可直接操作 `DataSourceHolder`：

```java
import com.zipe.base.database.DataSourceHolder;

// 切換至指定資料來源
DataSourceHolder.setDataSourceName("report");
try {
    // 執行資料庫操作
} finally {
    // 必須手動清除，否則執行緒池重用時會殘留此設定
    DataSourceHolder.clearDataSourceName();
}
```

:::warning ThreadLocal 殘留問題

`DynamicDataSourceAspect` 目前未在切面的 `finally` 區塊自動清除 ThreadLocal。若自行呼叫 `DataSourceHolder.setDataSourceName()`，**務必**在使用後呼叫 `DataSourceHolder.clearDataSourceName()`，否則 HTTP 執行緒池重用時，下個請求可能錯誤地使用前一個請求殘留的資料來源。

:::
