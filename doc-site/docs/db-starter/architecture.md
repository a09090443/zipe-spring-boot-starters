---
id: architecture
title: 架構與開發指南
sidebar_position: 5
---

# 架構與開發指南

本文件供接手或深度擴充 `db-spring-boot-starter` 的開發人員閱讀，完整說明模組的設計理念、內部結構與擴充方式。

---

## 1. 模組定位與設計理念

`db-spring-boot-starter` 解決的核心問題是：**在同一個 Spring Boot 應用程式中，以最小侵入性的方式同時管理多個資料庫連線，並允許業務程式碼在執行期間動態切換**。

### 設計目標

- **零侵入配置**：引用方只需加入 Maven 依賴，透過 `data-source.properties` 宣告資料來源，即可自動完成 HikariCP 連線池、JPA EntityManager、JdbcTemplate 等所有 Bean 的建立。
- **宣告式切換**：業務程式碼僅需在方法或類別上標注 `@DS("dsName")`，資料來源切換完全由 AOP 切面在框架層完成，不汙染業務邏輯。
- **SQL 外部化**：SQL 語句存放於 classpath 的 `.sql` 檔案，與 Java 程式碼分離，支援部署後直接修改外部 SQL 檔案而無需重新編譯。
- **統一 JDBC 封裝**：`BaseJDBC` 提供一致的查詢/更新 API，內建動態條件組裝（`Conditions`）與伺服器端分頁（`Paging`），降低重複的 JDBC 樣板程式碼。

### 技術選型

| 關注點 | 選用方案 | 原因 |
|---|---|---|
| 連線池 | HikariCP | Spring Boot 預設、效能最佳 |
| 動態路由 | `AbstractRoutingDataSource` | Spring 官方擴充點，無需第三方框架 |
| 切換觸發 | Spring AOP (`@Around`) | 在 `@Transactional` 之前執行（`@Order(-1)`） |
| SQL 監控 | P6Spy | JDBC 代理層攔截，無需修改原有 DataSource |
| 參數綁定 | `NamedParameterJdbcTemplate` | 具名參數（`:paramName`），可讀性與安全性優於位置佔位符 |

---

## 2. 套件結構

```
db-spring-boot-starter/
├── pom.xml
└── src/main/
    ├── java/com/zipe/
    │   ├── autoconfiguration/                         # Spring Boot AutoConfiguration 入口
    │   │   ├── DataSourceAspectAutoConfiguration.java # 向容器註冊 DynamicDataSourceAspect Bean
    │   │   └── DataSourceConfigAutoConfiguration.java # 建立所有 DataSource/JPA/JDBC Beans
    │   ├── base/
    │   │   ├── annotation/                            # 自訂 Annotation
    │   │   │   ├── DS.java                            # @DS：指定方法/類別使用的資料來源 key
    │   │   │   ├── DynamicDS.java                     # DS 的執行期實作，供反射替換 Annotation 用
    │   │   │   └── AnnotationHelper.java              # 利用反射在執行期修改類別上的 Annotation 值
    │   │   ├── aspect/
    │   │   │   └── DynamicDataSourceAspect.java       # AOP 切面：攔截 @DS，寫入 ThreadLocal
    │   │   ├── config/
    │   │   │   ├── DataSourcePropertyConfig.java      # @ConfigurationProperties(prefix="dynamic")
    │   │   │   └── P6SpyLogger.java                   # P6Spy 自訂日誌格式
    │   │   ├── database/
    │   │   │   ├── BaseDataSourceConfig.java          # 抽象父類：提供 HikariCP 基礎池設定
    │   │   │   ├── DataSourceHolder.java              # ThreadLocal 容器，儲存當前資料來源 key
    │   │   │   └── DynamicDataSource.java             # extends AbstractRoutingDataSource，路由至正確 DataSource
    │   │   └── model/
    │   │       └── DynamicDataSourceConfig.java       # 單一資料來源的屬性模型 (POJO)
    │   ├── common/model/
    │   │   └── SqlQuery.java                          # 查詢參數聚合 DTO（保留供外部擴充）
    │   ├── enums/
    │   │   └── ResourceEnum.java                      # SQL 檔案路徑描述 Enum
    │   └── jdbc/
    │       ├── BaseJDBC.java                          # 抽象父類：封裝所有 JDBC 查詢/更新操作
    │       └── criteria/
    │           ├── SQL.java                           # SQL 運算子常數 Enum
    │           ├── Conditions.java                    # 鏈式 WHERE 條件建構器
    │           ├── Paging.java                        # 抽象分頁資料類別
    │           └── Pair.java                          # 單一查詢條件的資料容器
    └── resources/
        ├── META-INF/spring/
        │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
        ├── application.yml                            # 模組預設設定
        ├── data-source.properties                     # dynamic.* 預設屬性範本
        └── spy.properties                             # P6Spy 靜態設定
```

### 各 package 職責摘要

| 套件 | 職責 |
|---|---|
| `autoconfiguration` | Spring Boot 自動配置入口；讀取屬性、建立 Bean，讓引用方零設定即可使用 |
| `base.annotation` | 定義 `@DS` 標記介面與其執行期動態實作 `DynamicDS`；`AnnotationHelper` 提供反射修改類別 Annotation 的工具 |
| `base.aspect` | AOP 切面，攔截 `@DS` 並在方法執行前將資料來源 key 寫入 ThreadLocal |
| `base.config` | 屬性綁定（`DataSourcePropertyConfig`）與 P6Spy 自訂日誌格式（`P6SpyLogger`） |
| `base.database` | 核心三件組：`BaseDataSourceConfig`（HikariCP 基礎設定）、`DataSourceHolder`（ThreadLocal）、`DynamicDataSource`（路由） |
| `base.model` | 單一資料來源連線設定的 POJO |
| `common.model` | SQL 查詢參數的包裝 DTO，供外部業務層組裝後傳遞使用 |
| `enums` | `ResourceEnum` 統一管理 SQL 檔案的目錄與副檔名 |
| `jdbc` | 公開 API 層：`BaseJDBC`（CRUD 操作封裝）與 `criteria`（動態條件/分頁建構） |

---

## 3. 核心類別詳解

### 3.1 AutoConfiguration 類別

#### `DataSourceConfigAutoConfiguration`

繼承 `BaseDataSourceConfig`，是整個模組最核心的配置類別。啟動時讀取 `data-source.properties` 中的 `dynamic.*` 屬性，為每個宣告的資料來源建立 HikariCP 連線池，組裝成 `DynamicDataSource`，並向 Spring 容器提供完整的資料存取 Bean 組合。

**關鍵 Bean 方法：**

| 方法 | 回傳型別 | 說明 |
|---|---|---|
| `dataSource()` | `DataSource` | 主要工廠方法，建立所有 HikariDataSource 並組裝 DynamicDataSource |
| `multiEntityManager(DataSource)` | `LocalContainerEntityManagerFactoryBean` | 建立 JPA EntityManagerFactory，掃描 `dynamic.entity-scan` 套件 |
| `transactionManager(...)` | `PlatformTransactionManager` | 建立 `@Primary JpaTransactionManager` |
| `jdbcTemplate(DataSource)` | `JdbcTemplate` | 供需要直接使用 JdbcTemplate 的程式碼注入 |
| `namedParameterJdbcDaoSupport(DataSource)` | `NamedParameterJdbcDaoSupport` | `BaseJDBC` 內部使用的具名參數 JDBC 支援 |

**私有 helper 方法：**

| 方法 | 說明 |
|---|---|
| `createDataSource(DynamicDataSourceConfig)` | 一般資料庫：`baseHikariConfig()` + 連線設定 |
| `createAs400DataSource(DynamicDataSourceConfig)` | AS400 專用：不帶 MySQL 優化設定，`connectionTestQuery` 改為 `VALUES 1` |
| `setHikariConnection(HikariConfig, DynamicDataSourceConfig)` | 共用邏輯：設定 url/username/password；若 `isEncrypt=true` 則解密密碼 |

#### `DataSourceAspectAutoConfiguration`

單純將 `DynamicDataSourceAspect` 以 `@Bean` 方式登錄到 Spring 容器，讓 AOP 可以正常運作。

---

### 3.2 Annotation 類別

#### `DS`（`com.zipe.base.annotation.DS`）

核心公開 Annotation，可標記於類別（`TYPE`）或方法（`METHOD`）層級。`value()` 填入 `data-source.properties` 中 `dynamic.data-source-map` 的 key 名稱，預設值為 `"common"`。

```java
// 方法層級：僅此方法使用 "report" 資料來源
@DS("report")
public List<Order> getReportData() { ... }

// 類別層級：整個類別的所有方法都走 "report"（優先於方法層級標注）
@DS("report")
public class ReportRepository { ... }
```

**優先規則：** 切面以「類別層級優先於方法層級」取用 Annotation。若類別上有 `@DS`，則忽略方法上的 `@DS`。

#### `DynamicDS` 與 `AnnotationHelper`

供進階使用：透過 Java 反射在執行期修改類別上已有 Annotation 的值，實現「根據業務邏輯動態決定資料來源 key」的需求。

> 注意：`AnnotationHelper` 依賴 JDK 內部實作，Java 9+ 需加 `--add-opens` JVM 啟動參數，否則可能拋出 `InaccessibleObjectException`。

---

### 3.3 動態切換核心三件組

#### `DynamicDataSourceAspect`（`com.zipe.base.aspect`）

動態切換機制的觸發點。

| 屬性 | 值 |
|---|---|
| Pointcut | `@within(DS)` 或 `@annotation(DS)` |
| Advice 類型 | `@Around` |
| 執行優先級 | `@Order(-1)`（在 `@Transactional` 之前執行） |

**關鍵邏輯（`around` 方法）：**
1. 呼叫 `getDSAnnotation(joinPoint)` — 先取類別上的 `@DS`，若無則取方法上的 `@DS`
2. 取得 `DS.value()` 作為資料來源 key
3. `DataSourceHolder.setDataSourceName(dsKey)` 寫入 ThreadLocal
4. `joinPoint.proceed()` 執行原方法

> **已知問題：** 目前 `around` 方法未在 `finally` 區塊呼叫 `DataSourceHolder.clearDataSourceName()`，執行緒池重用時存在 ThreadLocal 殘留的風險。詳見第 7 節「維護注意事項」。

#### `DataSourceHolder`（`com.zipe.base.database`）

以 `ThreadLocal<String>` 持有當前執行緒使用的資料來源 key；另以靜態 `List<String> dataSourceNames` 記錄所有已註冊的資料來源名稱（啟動時由 `DataSourceConfigAutoConfiguration` 填入）。

| 方法 | 說明 |
|---|---|
| `setDataSourceName(String)` | 設定當前執行緒使用的資料來源 |
| `getDataSourceName()` | 取得當前資料來源 key（未設定時回傳 null，使用 default） |
| `clearDataSourceName()` | 移除 ThreadLocal 值，防止記憶體洩漏 |
| `containsDataSource(String)` | 確認某名稱是否已在已知資料來源清單中 |

#### `DynamicDataSource`（`com.zipe.base.database`）

繼承 Spring 的 `AbstractRoutingDataSource`，覆寫 `determineCurrentLookupKey()` 回傳 `DataSourceHolder.getDataSourceName()`。Spring 據此從 `targetDataSources` Map 查找對應的 DataSource；若回傳 `null`，則退回 `defaultTargetDataSource`（`dynamic.primary` 指定的資料來源）。

---

### 3.4 屬性配置類別

#### `DataSourcePropertyConfig`（`com.zipe.base.config`）

以 `@ConfigurationProperties(prefix = "dynamic")` 綁定所有 `dynamic.*` 屬性，是屬性讀取的唯一入口。

| Java 欄位 | properties key | 型別 | 預設值 | 說明 |
|---|---|---|---|---|
| `primary` | `dynamic.primary` | `String` | 無 | 預設資料來源的 key 名稱 |
| `entityScan` | `dynamic.entity-scan` | `String` | 無 | JPA Entity 掃描套件路徑 |
| `isEncrypt` | `dynamic.is-encrypt` | `Boolean` | `false` | 密碼是否經 Base64 編碼；為 `true` 時模組以 Base64 解碼後再連線 |
| `dataSourceMap` | `dynamic.data-source-map[key].*` | `Map<String, DynamicDataSourceConfig>` | 無 | 各命名資料來源設定 |

#### `DynamicDataSourceConfig`（`com.zipe.base.model`）

單一資料來源連線設定的 POJO（Lombok `@Data`）。

| 欄位 | properties key 後綴 | 說明 |
|---|---|---|
| `name` | `.name` | 資料來源顯示名稱 |
| `url` | `.url` | JDBC URL（P6Spy 模式加 `p6spy:` 前綴） |
| `username` | `.username` | 資料庫帳號 |
| `pa55word` | `.pa55word` | 資料庫密碼（**注意：欄位名稱非 `password`**） |
| `driverClassName` | `.driverClassName` | JDBC 驅動類別 |

#### `BaseDataSourceConfig`（`com.zipe.base.database`）

提供 HikariCP 共用基礎設定（`@Bean baseHikariConfig()`），子類別可直接使用此設定建立每個 DataSource 的連線池。

**HikariCP 預設值（寫死於程式碼，無法透過 properties 覆蓋）：**

| 設定 | 預設值 | 說明 |
|---|---|---|
| `minimumIdle` | 5 | 最小閒置連線 |
| `maximumPoolSize` | 20 | 最大連線數 |
| `idleTimeout` | 30,000 ms | 閒置連線逾時 |
| `maxLifetime` | 2,000,000 ms（約 33 分鐘） | 連線最長生命週期 |
| `connectionTimeout` | 30,000 ms | 等待連線逾時 |
| `connectionTestQuery` | `SELECT 1` | 連線健康檢查（AS400 覆蓋為 `VALUES 1`） |
| `cachePrepStmts` | true | 預備語句快取 |
| `prepStmtCacheSize` | 250 | 預備語句快取數量 |
| `prepStmtCacheSqlLimit` | 2,048 | 單條語句最大長度 |
| `useServerPrepStmts` | true | Server-side Prepared Statements |

---

### 3.5 JDBC 操作層

#### `BaseJDBC`（`com.zipe.jdbc`）

抽象父類別，封裝所有 JDBC 讀寫操作。子類別只需繼承並加 `@Repository`，即可取得完整的 CRUD 能力。SQL 語句存放於外部 `.sql` 檔案，透過 `ResourceEnum` 定位，並以 `ConcurrentHashMap` 做檔案內容快取。

**SQL 外部檔案機制：**
- SQL 存放於 `src/main/resources/sql/<subdir>/<FILENAME>.sql`
- 透過 `ResourceEnum.SQL.getResource("FILENAME")` 或 `getResource("subdir", "FILENAME")` 組成路徑
- `getSqlText()` **優先從外部檔案系統讀取**（支援部署後熱換 SQL），若不存在才從 classpath 讀取
- 已讀取的 SQL 以 `cacheKey = enum.toString() + fileName + conditions.hashCode() + paging.hashCode()` 快取

**主要公開方法：**

| 方法 | 說明 |
|---|---|
| `update(ResourceEnum)` | 無參數更新 |
| `update(ResourceEnum, Map<String,Object>)` | 帶具名參數更新 |
| `updateBatch(ResourceEnum, List<Map<String,Object>>)` | 批量更新 |
| `queryForBean(ResourceEnum, Class<T>)` | 查詢單一 Bean（4 種多載） |
| `queryForObject(ResourceEnum, Map, Class<T>)` | 查詢單一純量值 |
| `queryForMap(ResourceEnum, ...)` | 查詢單筆 Map（4 種多載） |
| `queryForList(ResourceEnum, ...)` | 查詢多筆 Map（5 種多載，含分頁） |
| `queryForList(ResourceEnum, ..., Class<T>)` | 查詢多筆 Bean（5 種多載，含分頁） |

> 所有方法均使用 `NamedParameterJdbcTemplate` 具名參數（`:paramName`）綁定，禁止使用位置佔位符 `?`。

#### `ResourceEnum`（`com.zipe.enums`）

定義 SQL 檔案的存放慣例。目前只有一個值 `SQL`（dir=`/sql`，extension=`.sql`）。

```java
// 對應 classpath:/sql/USER_LIST.sql
ResourceEnum resource = ResourceEnum.SQL.getResource("USER_LIST");

// 對應 classpath:/sql/report/MONTHLY_SUMMARY.sql
ResourceEnum resource = ResourceEnum.SQL.getResource("report", "MONTHLY_SUMMARY");
```

> **注意：** `getResource()` 方法直接修改 Enum 實例的欄位（非執行緒安全），詳見第 7 節「維護注意事項」。

#### `Conditions`（`com.zipe.jdbc.criteria`）

鏈式建構 SQL WHERE 條件片段，以 `done(sqlText)` 將 SQL 檔案中的 `${CONDITIONS}` 佔位符替換為組裝好的條件串。條件**值**改以具名參數佔位符組裝並收集於 `getParameters()`（由 `BaseJDBC.mergeParams()` 併入綁定），**欄位名**則以白名單 `^[A-Za-z0-9_.]+$` 驗證。

| 方法 | 對應 SQL 片段 | 備注 |
|---|---|---|
| `equal(col, val)` | `col = :cN` | 值參數化；col 經白名單驗證 |
| `unEqual(col, val)` | `col <> :cN` | 同上 |
| `like(col, val)` | `col LIKE :cN` | 參數值為 `%val%` |
| `gt(col, val)` | `col > :cN` | |
| `lt(col, val)` | `col < :cN` | |
| `gtEqual(col, val)` | `col >= :cN` | |
| `ltEqual(col, val)` | `col <= :cN` | |
| `in(col, List)` | `col IN (:cN, :cM)` | 每個值各一具名參數 |
| `notIn(col, List)` | `col NOT IN (:cN, :cM)` | 每個值各一具名參數 |
| `isNull(col)` | `col IS NULL` | |
| `notNull(col)` | `col IS NOT NULL` | |
| `notExists(subquery)` | `NOT EXISTS ( subquery )` | 原生片段，勿傳使用者輸入 |
| `and()` | `AND` | |
| `or()` | `OR` | |
| `leftPT()` / `leftPT(SQL)` | `(` / `AND (` 等 | 括號分組 |
| `rightPT()` / `rightPT(SQL)` | `)` / `) AND` 等 | |
| `orderBy(col)` | `ORDER BY col ASC` | col 經白名單驗證 |
| `orderBy(col, SQL)` | `ORDER BY col DESC` 等 | col 經白名單驗證 |
| `rawSql(sql)` | 任意字串 | 原生片段，不跳脫，勿傳使用者輸入 |
| `getParameters()` | — | 回傳收集到的具名參數值 |
| `done(sqlText)` | 替換 `${CONDITIONS}` | 終結呼叫 |

#### `Paging`（`com.zipe.jdbc.criteria`）

抽象分頁資料類別（Lombok `@Data`），持有頁碼（`page`）、每頁筆數（`pagesize`）、總筆數（`recordsTotal` / `recordsFiltered`）與排序欄位（`List<String> orderBy`）。

分頁 SQL 模板在靜態初始化時從 classpath `/sql/PAGING.sql` 讀取並快取。`BaseJDBC.applyPaging()` 替換模板中的以下佔位符：

| 佔位符 | 說明 |
|---|---|
| `${QUERY_STRING}` | 原始查詢 SQL |
| `${ORDER_BY}` | 排序欄位（`Paging.orderBy` join 後） |
| `${START}` | 起始列號，計算式：`(page-1)*pagesize+1` |
| `${ENDED}` | 結束列號，計算式：`page*pagesize` |

> **重要：** 引用方必須在 `src/main/resources/sql/PAGING.sql` 提供適合其資料庫的分頁 SQL 模板，否則啟動時 `pageingTemplate` 為 `null`，使用分頁功能時會 NPE。

---

## 4. 核心協作流程

### 4.1 啟動期：資料來源初始化

```
Spring Boot 啟動
  → 掃描 META-INF/spring/AutoConfiguration.imports
  → DataSourceConfigAutoConfiguration 被載入
      → 讀取 data-source.properties（@PropertySource）
      → DataSourcePropertyConfig 綁定 dynamic.* 屬性
      → dataSource() Bean 方法被呼叫
          → 遍歷 dynamic.data-source-map
          → 依 URL 是否含 "as400" 選擇建立路徑
              → createDataSource(v) 或 createAs400DataSource(v)
                  → BaseDataSourceConfig.baseHikariConfig()
                  → setHikariConnection(config, v)
                      → 若 isEncrypt=true：CryptoUtil.decode(pa55word)
                  → new HikariDataSource(config)
          → DataSourceHolder.dataSourceNames.add(k)
      → new DynamicDataSource()
      → setTargetDataSources(dataSourceMap)
      → setDefaultTargetDataSource(dataSourceMap.get(primary))
      → afterPropertiesSet()  ← 觸發 AbstractRoutingDataSource 初始化
  → multiEntityManager(dataSource) → 建立 JPA EntityManagerFactory
  → transactionManager(...)        → 建立 @Primary JpaTransactionManager
  → jdbcTemplate(dataSource)       → 建立 JdbcTemplate
  → namedParameterJdbcDaoSupport() → 建立 NamedParameterJdbcDaoSupport

  → DataSourceAspectAutoConfiguration 被載入
  → getDynamicDataSourceAspect() → 建立 DynamicDataSourceAspect Bean
  → Spring AOP 對所有帶 @DS 的 Bean 建立代理
```

### 4.2 請求期：`@DS` 動態切換完整流程

以呼叫 `@DS("report") ReportService.getData()` 為例：

```
HTTP 請求進入 Controller
  → 呼叫 ReportService.getData()（被 Spring AOP 代理）
  → DynamicDataSourceAspect.around(joinPoint) 觸發（@Order(-1)）
      → getDSAnnotation(joinPoint)
          → targetClass.getAnnotation(DS.class) → 取到 @DS("report")
      → DataSourceHolder.setDataSourceName("report")
          → ThreadLocal.set("report")
      → joinPoint.proceed()
          → ReportService.getData() 真實執行
          → 呼叫 Repository 或 BaseJDBC 方法
          → DynamicDataSource.getConnection()
              → determineCurrentLookupKey()
                  → DataSourceHolder.getDataSourceName() → "report"
              → AbstractRoutingDataSource 查找 targetDataSources["report"]
              → 回傳 HikariDataSource for "report"
          → 執行 SQL（P6Spy 攔截 → Slf4J 日誌）
          → 回傳結果
  ← 注意：此處未呼叫 DataSourceHolder.clearDataSourceName()
```

### 4.3 `BaseJDBC` 查詢流程

```
業務 DAO（例如 UserJdbc extends BaseJDBC）

queryForList(ResourceEnum.SQL.getResource("USER_LIST"), params, conditions, UserDto.class)
  → BaseJDBC.queryForList(resource, params, conditions, null, clazz)
      → getSqlText(resource, conditions, null)
          → cacheKey = "SQL" + "USER_LIST" + conditions.hashCode()
          → SQL_CACHE.computeIfAbsent(cacheKey, k -> {
                → sqlPath = "/sql/USER_LIST.sql"
                → Files.exists(Paths.get(sqlPath)) ?
                    → 外部檔案系統讀取（熱換支援）
                  : FileUtil.getFileFromClasspath(sqlPath)
                    → classpath 讀取
                → sqlText = 檔案內容
                → conditions.done(sqlText)
                    → replace("${CONDITIONS}", conditionString)
              })
      → NamedParameterJdbcTemplate.query(sql, params, BeanPropertyRowMapper(clazz))
          → 底層 DynamicDataSource 決定連線
      → CollectionUtils.isEmpty(results) ? emptyList() : results
```

### 4.4 類別依賴關係圖

```mermaid
graph TD
    AutoConfigAspect[DataSourceAspectAutoConfiguration] -->|@Bean| Aspect[DynamicDataSourceAspect]
    AutoConfigDS[DataSourceConfigAutoConfiguration] -->|extends| BaseConfig[BaseDataSourceConfig]
    AutoConfigDS -->|reads| PropConfig[DataSourcePropertyConfig]
    PropConfig -->|contains Map| DSConfig[DynamicDataSourceConfig]
    AutoConfigDS -->|creates| DynDS[DynamicDataSource]
    AutoConfigDS -->|creates| HikariDS[HikariDataSource x N]
    DynDS -->|routes via| Holder[DataSourceHolder ThreadLocal]
    Aspect -->|writes| Holder
    DynDS -->|extends| AbstractRouting[AbstractRoutingDataSource]
    BaseJDBC -->|@Autowired| JdbcTmpl[JdbcTemplate]
    BaseJDBC -->|@Autowired| Named[NamedParameterJdbcDaoSupport]
    BaseJDBC -->|uses| ResourceEnum
    BaseJDBC -->|uses| Conditions
    BaseJDBC -->|uses| Paging
    Conditions -->|uses| Pair
    Conditions -->|uses| SQL_Enum[SQL enum]
    Paging -->|loads template| ResourceEnum
```

---

## 5. 自動配置運作原理

### 5.1 AutoConfiguration 入口

路徑：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.zipe.autoconfiguration.DataSourceConfigAutoConfiguration
com.zipe.autoconfiguration.DataSourceAspectAutoConfiguration
```

Spring Boot 3.x 啟動時掃描此檔案，依序載入兩個 AutoConfiguration 類別，使 Starter 在引用方無需任何額外設定的情況下自動生效。

### 5.2 條件註解

| 條件 | 位置 | 說明 |
|---|---|---|
| `@ConditionalOnClass(DataSourcePropertyConfig.class)` | `DataSourceConfigAutoConfiguration` | 只要引入本 Starter 即永遠成立（自身類別存在於 classpath） |

目前未使用 `@ConditionalOnMissingBean`，若引用方自行定義 `DataSource` Bean，需依賴 `spring.main.allow-bean-definition-overriding=true`（已在模組的 `application.yml` 設定）。

### 5.3 屬性綁定機制

```java
@AutoConfiguration
@PropertySource("classpath:data-source.properties")  // 載入模組預設屬性範本
@ConditionalOnClass(DataSourcePropertyConfig.class)
@EnableConfigurationProperties(DataSourcePropertyConfig.class)  // 啟用 @ConfigurationProperties 綁定
public class DataSourceConfigAutoConfiguration extends BaseDataSourceConfig { ... }
```

屬性讀取優先順序（依 Spring Boot 標準）：
1. 引用方專案的 `data-source.properties` 覆蓋模組內的範本
2. 引用方的 `application.yml` / `application.properties`
3. 模組內預設的 `data-source.properties`

### 5.4 P6Spy 整合

P6Spy 不透過 Spring AutoConfiguration 配置，而是透過 `spy.properties` 靜態設定：

```properties
module.log=com.p6spy.engine.logging.P6LogFactory,com.p6spy.engine.outage.P6OutageFactory
appender=com.p6spy.engine.spy.appender.Slf4JLogger
driverlist=com.microsoft.sqlserver.jdbc.SQLServerDriver,\
           com.ibm.as400.access.AS400JDBCDriver,\
           com.mysql.cj.jdbc.Driver
outagedetection=true
outagedetectioninterval=2   # 超過 2 秒標記為慢查詢
```

**啟用方式：** 將 JDBC URL 從 `jdbc:mysql://...` 改為 `jdbc:p6spy:mysql://...`，驅動改為 `com.p6spy.engine.spy.P6SpyDriver`。

---

## 6. 開發擴充指南

### 6.1 情境 A：建立一個使用 `@DS` 切換的業務 DAO（最常見）

這是最典型的使用情境，也是引用方開發人員最常執行的擴充。

**第一步：建立 SQL 檔案**

`src/main/resources/sql/USER_LIST.sql`：
```sql
SELECT
    u.id,
    u.username,
    u.email,
    u.created_at
FROM users u
WHERE 1 = 1
${CONDITIONS}
```

**第二步：建立 DAO 繼承 `BaseJDBC`**

```java
import com.zipe.enums.ResourceEnum;
import com.zipe.jdbc.BaseJDBC;
import com.zipe.jdbc.criteria.Conditions;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public class UserJdbc extends BaseJDBC {

    public List<Map<String, Object>> findActiveUsers() {
        Conditions conditions = new Conditions();
        conditions.equal("u.status", "ACTIVE")
                  .and()
                  .notNull("u.email");

        return queryForList(
            ResourceEnum.SQL.getResource("USER_LIST"),
            new java.util.HashMap<>(),
            conditions
        );
    }
}
```

**第三步：在 Service 以 `@DS` 指定資料來源**

```java
import com.zipe.base.annotation.DS;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserJdbc userJdbc;

    public UserService(UserJdbc userJdbc) {
        this.userJdbc = userJdbc;
    }

    // 使用 "report" 資料來源查詢
    @DS("report")
    public List<Map<String, Object>> getReportUsers() {
        return userJdbc.findActiveUsers();
    }
}
```

---

### 6.2 情境 B：新增支援一種資料庫驅動（例如 PostgreSQL）

**需修改的檔案：**

1. Starter 的 `pom.xml`（新增驅動依賴）
2. `src/main/resources/spy.properties`（在 `driverlist` 加入 PostgreSQL 驅動）

```xml
<!-- pom.xml 加入 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>
```

```properties
# spy.properties 的 driverlist 末尾加入
driverlist=com.microsoft.sqlserver.jdbc.SQLServerDriver,\
           com.ibm.as400.access.AS400JDBCDriver,\
           com.mysql.cj.jdbc.Driver,\
           org.postgresql.Driver
```

引用方的 `data-source.properties`：
```properties
dynamic.data-source-map[pg_db].url=jdbc:p6spy:postgresql://localhost:5432/mydb
dynamic.data-source-map[pg_db].username=postgres
dynamic.data-source-map[pg_db].pa55word=secret
dynamic.data-source-map[pg_db].driverClassName=com.p6spy.engine.spy.P6SpyDriver
```

---

### 6.3 情境 C：新增特殊資料庫的連線建立邏輯（類似 AS400 分支）

**需修改的檔案：** `DataSourceConfigAutoConfiguration.java`

```java
// 1. 新增 private 方法
private DataSource createOracleDataSource(DynamicDataSourceConfig dataSource) {
    HikariConfig config = setHikariConnection(new HikariConfig(), dataSource);
    config.setConnectionTestQuery("SELECT 1 FROM DUAL");
    // 其他 Oracle 特殊設定...
    return new HikariDataSource(config);
}

// 2. 在 dataSource() 方法的 forEach 內加入判斷
Optional.of(dynamicDataSource.getDataSourceMap()).ifPresent(ds -> ds.forEach((k, v) -> {
    DataSource created;
    String url = v.getUrl().toLowerCase();
    if (url.contains("as400")) {
        created = createAs400DataSource(v);
    } else if (url.contains("oracle")) {
        created = createOracleDataSource(v);   // 新增分支
    } else {
        created = createDataSource(v);
    }
    dataSourceMap.put(k, created);
    DataSourceHolder.dataSourceNames.add(k);
}));
```

---

### 6.4 情境 D：新增 `Conditions` 支援的條件類型（例如 `BETWEEN`）

**需修改的檔案：** `SQL.java`、`Conditions.java`（以及可能的 `Pair.java`）

```java
// 1. SQL.java：新增 Enum 常數
BETWEEN("BETWEEN");

// 2. Conditions.java：新增方法
// BETWEEN 需要兩個值，依照現有慣例各以 bindValue() 產生具名參數，避免 SQL Injection
public Conditions between(String column, Object start, Object end) {
    validateColumn(column);
    String startParam = bindValue(start);
    String endParam = bindValue(end);
    condition.append(column)
             .append(" BETWEEN :")
             .append(startParam)
             .append(" AND :")
             .append(endParam);
    return this;
}
```

使用範例：
```java
conditions.between("created_at", "2024-01-01", "2024-12-31")
          .and()
          .equal("status", "ACTIVE");
```

---

### 6.5 情境 E：使用 SQL 子目錄組織 SQL 檔案

無需修改模組程式碼。引用方的 DAO 使用二參數的 `getResource()`：

```java
// 對應 classpath:/sql/report/MONTHLY_SUMMARY.sql
ResourceEnum resource = ResourceEnum.SQL.getResource("report", "MONTHLY_SUMMARY");
List<Map<String, Object>> result = queryForList(resource, params);
```

SQL 檔案放置於：`src/main/resources/sql/report/MONTHLY_SUMMARY.sql`

---

### 6.6 最小引用方配置骨架

```
業務專案/
├── pom.xml                           # 引入 db-spring-boot-starter 依賴
└── src/main/
    ├── resources/
    │   ├── data-source.properties    # dynamic.* 設定
    │   └── sql/
    │       ├── USER_LIST.sql         # SELECT ... WHERE ${CONDITIONS}
    │       └── PAGING.sql            # 分頁 SQL 模板（使用分頁功能時必填）
    └── java/com/example/
        ├── model/User.java           # @Entity 類別
        ├── repository/
        │   └── UserRepository.java   # extends JpaRepository
        ├── jdbc/
        │   └── UserJdbc.java         # @Repository, extends BaseJDBC
        └── service/
            └── UserService.java      # @DS("xxx") 標注方法
```

最小 `data-source.properties`：
```properties
dynamic.primary=db1
dynamic.entity-scan=com.example
dynamic.is-encrypt=false
dynamic.data-source-map[db1].url=jdbc:p6spy:mysql://localhost:3306/mydb?serverTimezone=Asia/Taipei
dynamic.data-source-map[db1].username=user
dynamic.data-source-map[db1].pa55word=pass
dynamic.data-source-map[db1].driverClassName=com.p6spy.engine.spy.P6SpyDriver
```

MySQL `PAGING.sql` 範例（SQL Server / AS400 語法不同，需另行調整）：
```sql
SELECT * FROM (
    SELECT ROW_NUMBER() OVER (ORDER BY ${ORDER_BY}) AS ROW_NUM, T.* FROM (
        ${QUERY_STRING}
    ) T
) PAGED_RESULT
WHERE ROW_NUM BETWEEN ${START} AND ${ENDED}
```

---

## 7. 維護注意事項與常見陷阱

### 7.1 ThreadLocal 未清除（高風險）

**位置：** `DynamicDataSourceAspect.around()`

`around` 方法在 `joinPoint.proceed()` 後**沒有** `finally { DataSourceHolder.clearDataSourceName(); }`。在 Tomcat / Undertow 的 HTTP 工作執行緒池環境中，執行緒被重用：
- 第一個請求呼叫 `@DS("report")` → ThreadLocal = `"report"`
- 第二個請求沒有 `@DS` 標記 → ThreadLocal 殘留 `"report"` → 錯誤地使用 `"report"` 資料來源

**建議修復：**

```java
@Around("dataSourcePointCut()")
public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    String dsKey = getDSAnnotation(joinPoint).value();
    DataSourceHolder.setDataSourceName(dsKey);
    try {
        return joinPoint.proceed();
    } finally {
        DataSourceHolder.clearDataSourceName();  // 務必加入
    }
}
```

若手動使用 `DataSourceHolder.setDataSourceName()`，同樣必須自行在 finally 呼叫 `clearDataSourceName()`。

---

### 7.2 `ResourceEnum` 非執行緒安全（潛在並發問題）

**位置：** `ResourceEnum.getResource(String, String)`

`getResource()` 方法直接修改 Enum 單例的 `dir` / `file` 欄位（非執行緒安全）。在高並發下，兩個執行緒同時呼叫不同的 `getResource()` 可能導致路徑錯亂。

**現有緩解措施：** `SQL_CACHE.computeIfAbsent()` 確保第一次正確解析後直接使用快取，但第一次並發時仍有風險。

**建議修復：** 將 `getResource()` 改為回傳新物件（Value Object）而非修改 Enum 自身；或在 `getSqlText()` 中對 `ResourceEnum` 操作加 `synchronized`。

---

### 7.3 條件值已參數化（SQL Injection 已修復）

**位置：** `Conditions.appendPairTypes()`

`Conditions` 的所有條件**值**（含 `IN` / `NOT IN` 的每個元素）已改為產生具名參數佔位符（`:c0`、`:c1`…）並收集於 `getParameters()`，由 `BaseJDBC.mergeParams()` 併入後交 `NamedParameterJdbcTemplate` 綁定，**不再字串拼接**，因此條件值可安全接受使用者輸入。

仍需注意：

- **欄位名稱**無法參數化，會以白名單規則 `^[A-Za-z0-9_.]+$` 驗證，非法時丟 `IllegalArgumentException`；欄位名請勿來自未驗證的使用者輸入。
- `rawSql()` 與 `notExists()` 為原生 SQL 片段，不做跳脫或參數化，嚴禁傳入使用者輸入。

---

### 7.4 使用分頁功能前必須放置 `PAGING.sql`

**位置：** `Paging` 靜態初始化區塊

若 classpath 中沒有 `/sql/PAGING.sql`，`pageingTemplate` 會是 `null`，使用分頁功能時會拋出 NPE。此依賴無任何啟動期驗證。

**不同資料庫的分頁語法不同：**
- MySQL：使用 `LIMIT` / `OFFSET`
- SQL Server：使用 `OFFSET ... FETCH`
- AS400：使用 `FETCH FIRST ... ROWS ONLY`

---

### 7.5 密碼欄位名稱為 `pa55word`（非 `password`）

**位置：** `DynamicDataSourceConfig.pa55word`

密碼欄位故意使用 `pa55word` 以避開部分自動安全掃描工具的偵測。`data-source.properties` 中的 key 必須完全對應：

```properties
# 正確
dynamic.data-source-map[db1].pa55word=mypassword

# 錯誤（屬性綁定失敗，密碼為空）
dynamic.data-source-map[db1].password=mypassword
```

---

### 7.6 `@DS` 對類別內部 `this` 呼叫無效

這是 Spring AOP 代理的通用限制。`@DS` 僅對「跨 Bean 的外部呼叫」生效：

```java
@Service
public class OrderService {

    @DS("report")
    public void methodA() {
        // 正確：外部呼叫時切面會觸發
    }

    public void methodB() {
        this.methodA();  // 切面不觸發！仍使用預設資料來源
    }
}
```

**解決方式：** 將需要切換資料來源的邏輯拆分到另一個 Bean，再跨 Bean 呼叫；或直接手動呼叫 `DataSourceHolder.setDataSourceName()`。

---

### 7.7 `@DS` 優先級：類別層級優先於方法層級

切面的 `getDSAnnotation()` 實作為：先取類別上的 `@DS`，若有則直接使用，**不再讀取方法層級的 `@DS`**。

```java
// 類別上有 @DS("common")，則所有方法（包括標注 @DS("report") 的方法）都走 "common"
@DS("common")
@Repository
public class UserRepository {

    @DS("report")  // 此處無效！類別層級優先
    public List<User> findAll() { ... }
}
```

---

### 7.8 AnnotationHelper 在 Java 9+ 的相容性

`AnnotationHelper` 使用反射存取 JDK 私有欄位，Java 9+ 的強封裝模組系統下需在 JVM 啟動時加入：

```
--add-opens java.base/java.lang=ALL-UNNAMED
```

否則可能拋出 `InaccessibleObjectException`。

---

### 7.9 Starter 內建四種資料庫驅動（非 optional）

`pom.xml` 中以下驅動為 `compile` scope（非 `optional`），引入 Starter 後會一併帶入：
- `mssql-jdbc`（SQL Server）
- `mysql-connector-j`（MySQL）
- `mariadb-java-client`（MariaDB）
- `jt400`（AS400）

若業務專案不需要某些驅動，可在業務專案的 `pom.xml` 以 `<exclusion>` 排除。

---

### 7.10 SQL 快取對動態條件的限制

`getSqlText()` 的 cacheKey 組裝：

```java
String cacheKey = resource.toString() + resource.fileName()
    + (conditions != null ? conditions.hashCode() : "")
    + (paging != null ? paging.hashCode() : "");
```

`conditions.hashCode()` 使用 `Object.hashCode()`（記憶體地址），每個 `new Conditions()` 實例都是不同的 key，**帶 Conditions 的 SQL 快取命中率接近零**，只有靜態 SQL（無 Conditions）才能被有效快取。若想讓動態條件 SQL 也被快取，需在 `Conditions` 類別實作 `equals()` / `hashCode()`。
