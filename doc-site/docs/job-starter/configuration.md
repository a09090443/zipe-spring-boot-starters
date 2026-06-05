---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `job-spring-boot-starter` 的所有可設定屬性，涵蓋排程啟用、儲存模式、JDBC 資料來源與連線池設定。

:::important 正確的命名空間
本模組讀取的是 **`spring.quartz.*`** 與 **`spring.datasource.quartz.*`** 命名空間，而非 `zipe.quartz.*`。JDBC 資料來源設定請使用 `spring.datasource.quartz.*`。
:::

---

## 排程啟用與基本屬性

這些屬性由 Spring Boot Quartz 整合與 `spring-quartz.properties`（模組內建預設值）共同控制。消費方可在 `application.yml` 覆寫。

| 屬性鍵 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `spring.quartz.enable` | Boolean | `true` | 是否啟用 `InitialJobAutoConfiguration`（控制整個 starter 的啟用） |
| `spring.quartz.job-store-type` | String | `memory` | 儲存模式：`memory`（記憶體）或 `jdbc`（持久化） |
| `spring.quartz.auto-startup` | Boolean | `true` | 應用啟動時是否自動啟動排程器 |
| `spring.quartz.startup-delay` | Integer | `0` | 排程器延遲啟動秒數 |
| `spring.quartz.overwrite-existing-jobs` | Boolean | `false` | 是否覆寫已存在的同名排程 |
| `spring.quartz.scheduler-name` | String | `jobScheduler` | 排程器名稱 |
| `spring.quartz.wait-for-jobs-to-complete-on-shutdown` | Boolean | `false` | 關閉時是否等待正在執行的 Job 完成 |
| `spring.quartz.jdbc.initialize-schema` | String | `always` | JDBC 模式的建表策略：`always` / `embedded` / `never` |

:::danger `initialize-schema=always` 的生產風險
預設值 `always` 會在**每次啟動**時重建 `QRTZ_*` 資料表，清除所有持久化的排程資料。JDBC 正式環境首次建表後，務必覆寫為 `never`：

```yaml
spring:
  quartz:
    jdbc:
      initialize-schema: never
```
:::

---

## 批次排程定義屬性（`quartz-jobs.properties`）

`InitialJobAutoConfiguration` 在應用啟動時讀取以下格式的 properties，自動批次建立 Cron 排程。消費方需在 `src/main/resources/` 提供 `quartz-jobs.properties` 檔案。

| 屬性鍵格式 | 型別 | 說明 |
|---|---|---|
| `quartz.job-map[KEY].name` | String | 排程唯一識別名稱（JobKey 的 name 部分） |
| `quartz.job-map[KEY].description` | String | 排程描述 |
| `quartz.job-map[KEY].group` | String | 排程群組（目前實際建立時 group 固定為 `"file"`，此欄位保留供未來修正） |
| `quartz.job-map[KEY].clazz` | String | Job 的完整類別名稱（fully qualified class name） |
| `quartz.job-map[KEY].cronExpression` | String | Cron 表示式（6 至 7 欄位 Quartz 格式） |

範例：

```properties
# quartz-jobs.properties
quartz.job-map[DailyReport].name=DailyReport
quartz.job-map[DailyReport].description=每日報表排程
quartz.job-map[DailyReport].group=schedule
quartz.job-map[DailyReport].clazz=com.example.job.DailyReportJob
quartz.job-map[DailyReport].cronExpression=0 0 2 * * ?

quartz.job-map[DataSync].name=DataSync
quartz.job-map[DataSync].description=資料同步排程
quartz.job-map[DataSync].group=schedule
quartz.job-map[DataSync].clazz=com.example.job.DataSyncJob
quartz.job-map[DataSync].cronExpression=0 0/15 * * * ?
```

:::note 批次初始化只支援 Cron 觸發器
`InitialJobAutoConfiguration` 目前僅支援以 `cronExpression` 定義的 Cron 觸發器。若需固定間隔觸發，請改用 REST API（`POST /quartz/register`）動態註冊。
:::

---

## JDBC 模式資料來源屬性

當 `spring.quartz.job-store-type=jdbc` 時，需在消費方 `src/main/resources/` 提供 `quartz-datasource.properties` 檔案（`DataSourceAutoConfiguration` 透過 `@PropertySource` 載入此檔案）。

| 屬性鍵 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `spring.datasource.quartz.url` | String | 無 | Quartz 資料庫 JDBC URL | 是 |
| `spring.datasource.quartz.username` | String | 無 | 資料庫帳號 | 是 |
| `spring.datasource.quartz.password` | String | 無 | 資料庫密碼 | 是 |
| `spring.datasource.quartz.driver-class-name` | String | 無 | JDBC 驅動類別名稱 | 是 |

`quartz-datasource.properties` 範例：

```properties
# 由消費方在 src/main/resources/quartz-datasource.properties 提供
spring.datasource.quartz.username=quartz_user
spring.datasource.quartz.password=your_password
spring.datasource.quartz.url=jdbc:mysql://localhost:3306/quartz_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Taipei
spring.datasource.quartz.driver-class-name=com.mysql.cj.jdbc.Driver
```

:::warning `quartz-datasource.properties` 需由消費方自行提供
此檔案**不在** starter jar 內，`DataSourceAutoConfiguration` 以 `@PropertySource("classpath:quartz-datasource.properties")` 載入。若使用 JDBC 模式但未提供此檔案，DataSource 屬性值為空，應用啟動時將報錯。
:::

---

## HikariCP 連線池屬性

`DataSourceAutoConfiguration` 建立的 `HikariDataSource` 可透過 `spring.datasource.hikari.*` 屬性調整。模組在 `hikari-config.properties` 中設有預設值，消費方可在 `application.yml` 覆寫。

常用屬性：

| 屬性鍵 | 型別 | 說明 |
|---|---|---|
| `spring.datasource.hikari.maximum-pool-size` | Integer | 最大連線數 |
| `spring.datasource.hikari.minimum-idle` | Integer | 最小閒置連線數 |
| `spring.datasource.hikari.connection-timeout` | Long | 取得連線的最長等待毫秒數 |
| `spring.datasource.hikari.idle-timeout` | Long | 閒置連線的最長保留毫秒數 |
| `spring.datasource.hikari.max-lifetime` | Long | 連線的最長存活毫秒數 |

---

## Quartz 原生屬性（JDBC 模式）

以下為模組內建 `quartz.properties` 的預設值。JDBC 模式下生效，消費方可在 classpath 提供同名檔案覆寫。

```properties
org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
org.quartz.jobStore.tablePrefix=QRTZ_
org.quartz.jobStore.dataSource=quartzDataSource
org.quartz.threadPool.threadCount=15
org.quartz.threadPool.threadPriority=5
```

叢集模式額外需要以下屬性（需消費方自行覆寫 `quartz.properties`）：

```properties
org.quartz.jobStore.isClustered=true
org.quartz.jobStore.clusterCheckinInterval=15000
org.quartz.scheduler.instanceId=AUTO
org.quartz.scheduler.instanceName=ClusteredScheduler
```

---

## 完整 application.yml 範例

### 記憶體模式（開發/測試環境）

```yaml
spring:
  quartz:
    enable: true
    job-store-type: memory
    auto-startup: true
    startup-delay: 0
    overwrite-existing-jobs: false
    scheduler-name: jobScheduler
    wait-for-jobs-to-complete-on-shutdown: false
```

### JDBC 模式（正式環境）

```yaml
spring:
  quartz:
    enable: true
    job-store-type: jdbc
    auto-startup: true
    startup-delay: 5
    overwrite-existing-jobs: false
    scheduler-name: jobScheduler
    wait-for-jobs-to-complete-on-shutdown: true
    jdbc:
      initialize-schema: never   # 資料表已建立後設為 never，避免重建清除資料
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
```

以及消費方 `src/main/resources/quartz-datasource.properties`：

```properties
spring.datasource.quartz.username=quartz_user
spring.datasource.quartz.password=${QUARTZ_DB_PASSWORD}
spring.datasource.quartz.url=jdbc:mysql://localhost:3306/quartz_db?useSSL=false&serverTimezone=Asia/Taipei
spring.datasource.quartz.driver-class-name=com.mysql.cj.jdbc.Driver
```

:::note 記憶體模式的取捨
記憶體模式無須建表、啟動最快，但所有排程會在應用重啟後遺失，且無法在多節點間共享，僅適合單機或開發測試環境。正式環境的持久化與叢集需求請使用 JDBC 模式。
:::

:::info 叢集部署
JDBC 模式搭配 Quartz 的叢集設定，可在多節點環境中避免同一排程被重複觸發。如有此需求，請在消費方覆寫 `quartz.properties` 加入 `org.quartz.jobStore.isClustered=true` 等屬性。
:::

:::warning JDBC 模式需先建表
使用 JDBC 模式前，必須先在目標資料庫執行 Quartz 官方提供的建表腳本（依資料庫類型命名，如 `tables_mysql_innodb.sql`），建立 `QRTZ_*` 系列資料表，否則排程器啟動時會失敗。
:::
