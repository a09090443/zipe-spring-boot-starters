---
id: architecture
title: 架構與開發指南
sidebar_position: 5
---

# 架構與開發指南

本文件面向負責維護或擴充 `job-spring-boot-starter` 的開發人員，說明模組的設計理念、套件結構、核心類別關係、協作流程，以及常見的擴充情境與陷阱。

---

## 1. 模組定位與設計理念

`job-spring-boot-starter` 是一個基於 **Quartz Scheduler** 的 Spring Boot AutoConfiguration 模組，目標是讓業務系統以最低成本引入完整的排程管理能力：

- **AutoConfiguration 優先**：透過 Spring Boot SPI 機制自動載入，引入 jar 即生效，無需繁瑣的手動 Bean 宣告。
- **雙模式設計**：記憶體（RAMJobStore）與 JDBC 持久化兩種模式以設定值切換，無需改程式碼。
- **REST 即管理介面**：`QuartzController` 繼承 `BaseJob`，直接將排程操作暴露為 HTTP API，不需要額外的管理後台。
- **Template Method 封裝執行框架**：`QuartzJobFactory` 以 Template Method 模式封裝 before/after/error 日誌，業務 Job 只需覆寫 `executeJob()`，不需自行處理日誌與例外。
- **屬性驅動的批次初始化**：`InitialJobAutoConfiguration` 在啟動時讀取 `quartz-jobs.properties`，自動向 Scheduler 批次註冊排程，適合固定排程清單的場景。

---

## 2. 套件結構

```
job-spring-boot-starter/
├── pom.xml                                              # groupId=io.github.a09090443, version=3.5.11.0
└── src/main/
    ├── java/com/zipe/
    │   ├── Application.java                             # 模組內建獨立啟動入口（開發/測試用）
    │   └── quartz/
    │       ├── autoconfiguration/                       # Spring Boot AutoConfiguration 入口
    │       │   ├── DataSourceAutoConfiguration.java     # 條件式 Quartz 專屬 DataSource 自動配置
    │       │   └── InitialJobAutoConfiguration.java     # 讀取 properties 自動批次建立排程
    │       ├── base/
    │       │   └── BaseJob.java                         # 排程生命週期管理抽象類別（供子類繼承）
    │       ├── config/
    │       │   ├── QuartzDataSourceProperties.java      # 綁定 spring.datasource.quartz.* 屬性
    │       │   └── QuartzJobPropertyConfig.java         # 綁定 quartz.job-map.* 屬性（批次排程定義）
    │       ├── controller/
    │       │   └── QuartzController.java                # REST API：POST /quartz/register|delete|pause|resume|run
    │       ├── enums/
    │       │   ├── ScheduleEnum.java                    # 時間單位 + 觸發器建構（NOW/SECOND/.../CRON）
    │       │   └── ScheduleJobStatusEnum.java           # 排程操作狀態碼（MERGE/DELETE/PAUSE/RESUME/ONCE 等）
    │       ├── job/
    │       │   ├── HelloWorldJob.java                   # @Scheduled 範例（非 Quartz，每 20 秒執行）
    │       │   ├── QuartzJobFactory.java                # Quartz Job 執行框架（Template Method）
    │       │   └── TestJob.java                         # QuartzJobFactory 範例實作
    │       ├── model/
    │       │   └── Job.java                             # 排程領域模型（兼 @ConfigurationProperties）
    │       ├── util/
    │       │   └── QuartzJobUtil.java                   # 建構工具：buildJobDetail / buildJobTrigger
    │       └── vo/
    │           └── ScheduleJobVO.java                   # REST API 請求/回應 View Object
    └── resources/
        ├── application.yml                              # 模組預設設定（job-store-type=memory）
        ├── spring-quartz.properties                     # Spring Boot Quartz 整合屬性
        ├── quartz.properties                            # Quartz 原生 jobStore/threadPool 設定（JDBC 模式）
        ├── hikari-config.properties                     # HikariCP 連線池預設值
        ├── quartz-jobs.properties                       # 批次排程定義（quartz.job-map[key].* 格式）
        └── META-INF/spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
                                                         # SPI 入口：登錄兩個 AutoConfiguration 類別
```

### 各 package 職責

| Package | 職責 |
|---|---|
| `autoconfiguration` | Spring Boot SPI 進入點，依條件啟用資料來源或排程初始化 |
| `base` | 提供 `BaseJob` 供業務層或 Controller 繼承，封裝所有 Quartz Scheduler 操作邏輯 |
| `config` | 屬性綁定類別，將外部 properties/yaml 對映為強型別物件 |
| `controller` | HTTP 層，繼承 `BaseJob`，將五個操作方法直接暴露為 REST 端點 |
| `enums` | 定義觸發器類型（`ScheduleEnum`）與操作意圖（`ScheduleJobStatusEnum`）兩套語義 |
| `job` | 執行框架：`QuartzJobFactory` 為 Template Method 框架，`HelloWorldJob`/`TestJob` 為示範 |
| `model` | 排程領域物件 `Job`，帶 `JobDataMap` 支援任意 key-value 參數傳遞 |
| `util` | 純工具層，依賴 Quartz API 建構 `JobDetail` 與 `Trigger`，不持有排程器參照、不持有狀態 |
| `vo` | 對外傳輸物件，欄位名稱與 `model.Job` 不同（如 `jobName` vs `name`），作 JSON 序列化使用 |

---

## 3. 核心類別詳解

### 3.1 `BaseJob`

**完整路徑：** `com.zipe.quartz.base.BaseJob`

排程管理的抽象基底類別，封裝所有 Quartz `Scheduler` 操作。`QuartzController` 直接繼承此類，業務層若需自訂操作排程也可繼承。

| 方法 | 存取層級 | 說明 |
|---|---|---|
| `BaseJob(Scheduler scheduler)` | `@Autowired protected` | 透過建構子注入 Scheduler，子類無需再宣告 |
| `mergeJobProcess(ScheduleJobVO)` | `protected` | 新增或覆蓋更新排程（upsert 語義） |
| `deleteJobProcess(ScheduleJobVO)` | `protected` | 刪除排程（JobKey = jobName + jobGroup） |
| `pauseJobProcess(ScheduleJobVO)` | `protected` | 暫停排程 |
| `resumeJobProcess(ScheduleJobVO)` | `protected` | 恢復已暫停的排程 |
| `runJobProcess(ScheduleJobVO)` | `protected` | 立即執行一次（jobName 加 `-Once` 後綴，防止與常駐排程衝突） |
| `scheduleJobStatusProcess(ScheduleJobVO, ScheduleJobStatusEnum)` | `private` | 統一分派中心，switch 到對應 Scheduler 操作 |
| `createJob(Job, ScheduleBuilder)` | `private` | 先 unscheduleJob（如已存在），再 scheduleJob(replace=true) |
| `executeOnce(Job, ScheduleBuilder)` | `private` | 在 jobName 後附加 `-Once` 後再呼叫 createJob |
| `convertToJob(ScheduleJobVO)` | `private` | 將 VO 轉為內部 Job 模型（欄位映射） |

**MERGE 分支決策：**
- 若 `ScheduleJobVO.cronExpression` 不為空 → 使用 `ScheduleEnum.CRON.setExpression(cron)` 建立 CronTrigger
- 否則 → 使用 `ScheduleEnum.getTimeUnit(timeUnit).setCycle(repeatInterval)` 建立固定間隔 Trigger

**ONCE 分支特性：**
- 強制使用 `ScheduleEnum.NOW`（`SimpleScheduleBuilder.withMisfireHandlingInstructionFireNow()`）
- `startTime` 設為 null（立即啟動），jobName 後附加 `-Once`

---

### 3.2 `QuartzJobFactory`

**完整路徑：** `com.zipe.quartz.job.QuartzJobFactory`

所有 Quartz Job 的執行框架基類，繼承 Spring 的 `QuartzJobBean`，提供 Template Method 模式。

| 方法 | 說明 |
|---|---|
| `executeInternal(JobExecutionContext)` | Quartz 呼叫入口，依序呼叫 before → executeJob → after/error |
| `beforeExecuteJobLog(context)` | `protected`，記錄排程名稱與開始時間 |
| `afterExecuteJobLog(context)` | `protected`，記錄排程名稱與結束時間 |
| `errorExecuteJobLog(context, e)` | `protected`，記錄排程名稱與例外訊息（log.error） |
| `executeJob(JobExecutionContext)` | `protected abstract`，子類必須實作的業務方法 |

**關鍵設計：** 繼承 `QuartzJobBean` 而非直接實作 `Job` 介面。`QuartzJobBean` 在每次執行前會將 `JobDataMap` 中的值注入 Bean 屬性（若屬性名稱相符）。業務 Job 只需覆寫 `executeJob()`。

:::warning 注意：`BaseJob` 與 `QuartzJobFactory` 的分工
- **`BaseJob`**：管理排程的生命週期（新增、暫停、恢復、刪除），不負責執行邏輯。
- **`QuartzJobFactory`**：定義排程被 Quartz 觸發時的執行框架，業務 Job 繼承此類。

兩者**不相關**，**不要混用**。業務 Job 繼承 `QuartzJobFactory`，而非 `BaseJob`。
:::

---

### 3.3 `QuartzJobUtil`

**完整路徑：** `com.zipe.quartz.util.QuartzJobUtil`

純工具類別，依賴 Quartz `JobBuilder` 與 `TriggerBuilder`，依傳入的 `Job` 模型建構 `JobDetail` 與 `Trigger`。不持有 Scheduler 參照，不執行任何排程操作。

| 方法 | 說明 |
|---|---|
| `QuartzJobUtil()` | 無參建構子 |
| `QuartzJobUtil(Job job)` | 帶 Job 的建構子（儲存為 field） |
| `buildJobDetail()` | 使用 field job 呼叫下方多載 |
| `buildJobDetail(Job job)` | 以 `Class.forName(job.getClazz())` 動態載入，建構帶 identity/description/dataMap 的 JobDetail（`storeDurably()`） |
| `buildJobTrigger(ScheduleBuilder builder)` | 便捷多載，自動建構 JobDetail 後呼叫三參版 |
| `buildJobTrigger(JobDetail, Job, ScheduleBuilder)` | 核心方法：依 Job.startTime/endTime 建構帶 startAt/endAt 的 Trigger |

:::warning `Class.forName()` 的要求
`buildJobDetail` 使用 `Class.forName()`，需要 Job class 在 classpath 且完整類別名稱正確，否則拋 `ClassNotFoundException`。請確保 `ScheduleJobVO.jobClass` 填入完整的套件路徑名稱。
:::

---

### 3.4 `ScheduleEnum`

**完整路徑：** `com.zipe.quartz.enums.ScheduleEnum`

封裝 Quartz 三種 ScheduleBuilder 的建立邏輯，以整數 `timeUnit` 作為 lookup key。

| 常數 | timeUnit 值 | ScheduleBuilder 類型 |
|---|---|---|
| `NOW` | 0 | `SimpleScheduleBuilder.withMisfireHandlingInstructionFireNow()` |
| `SECOND` | 1 | `SimpleScheduleBuilder.withIntervalInSeconds(n).repeatForever()` |
| `MINUTE` | 2 | `SimpleScheduleBuilder.withIntervalInMinutes(n).repeatForever()` |
| `HOUR` | 3 | `SimpleScheduleBuilder.withIntervalInHours(n).repeatForever()` |
| `DAY` | 4 | `CalendarIntervalScheduleBuilder.withIntervalInDays(n)` |
| `WEEK` | 5 | `CalendarIntervalScheduleBuilder.withIntervalInWeeks(n)` |
| `MONTH` | 6 | `CalendarIntervalScheduleBuilder.withIntervalInMonths(n)` |
| `YEAR` | 7 | `CalendarIntervalScheduleBuilder.withIntervalInYears(n)` |
| `CRON` | （無獨立 timeUnit） | `CronScheduleBuilder.cronSchedule(expression)` |

關鍵方法：
- `setCycle(int interval)` → 回傳對應時間單位的 ScheduleBuilder（NOW/SECOND~YEAR 使用此方法）
- `setExpression(String timeExpression)` → 僅 CRON 使用，回傳 CronScheduleBuilder
- `getTimeUnit(int timeUnit)` → 靜態 lookup，根據整數 timeUnit 回傳對應 enum 常數

:::caution CRON 無法透過 getTimeUnit() 取得
`CRON` 未傳入獨立 timeUnit 值，其預設 `timeUnit` 欄位與 `NOW` 同為 0。因此 `ScheduleEnum.getTimeUnit(0)` 永遠回傳 `NOW`，而非 `CRON`。若需使用 Cron 觸發，必須在 `ScheduleJobVO.cronExpression` 欄位填入 Cron 表示式，`BaseJob` 的 MERGE 分支會自動判斷並走 CRON 路徑。
:::

---

### 3.5 `ScheduleJobStatusEnum`

**完整路徑：** `com.zipe.quartz.enums.ScheduleJobStatusEnum`

定義排程操作意圖與運行狀態的統一語義碼。

| 常數 | status 值 | 語義 |
|---|---|---|
| `DELETE` | 0 | 刪除排程 |
| `START` | 1 | 啟動（保留，目前 switch 未使用） |
| `PAUSE` | 2 | 暫停排程 |
| `RESUME` | 3 | 恢復排程 |
| `MERGE` | 4 | 新增或更新（upsert） |
| `RUNNING` | 5 | 執行中（保留狀態） |
| `ERROR` | 6 | 錯誤狀態（保留） |
| `COMPLETE` | 7 | 完成（保留） |
| `ONCE` | 8 | 立即執行一次 |

`BaseJob.scheduleJobStatusProcess()` 的 switch 僅使用 MERGE/DELETE/PAUSE/RESUME/ONCE 五個操作意圖，其餘為保留狀態供未來擴充。

---

### 3.6 `QuartzController`

**完整路徑：** `com.zipe.quartz.controller.QuartzController`

繼承 `BaseJob`，將五個排程操作暴露為 HTTP POST 端點，接受/回傳 `ScheduleJobVO` JSON。

| HTTP 端點 | 說明 |
|---|---|
| `POST /quartz/register` | 呼叫 `mergeJobProcess()`，新增或覆蓋排程 |
| `POST /quartz/delete` | 呼叫 `deleteJobProcess()` |
| `POST /quartz/pause` | 呼叫 `pauseJobProcess()` |
| `POST /quartz/resume` | 呼叫 `resumeJobProcess()` |
| `POST /quartz/run` | 呼叫 `runJobProcess()`，立即執行一次 |

所有端點接受 `@RequestBody ScheduleJobVO`（JSON），並回傳原始 VO（含錯誤時的 `message` 欄位）。**HTTP 狀態碼永遠為 200**，需自行檢查回應 body 的 `message` 欄位判斷是否失敗。

---

### 3.7 `ScheduleJobVO`

**完整路徑：** `com.zipe.quartz.vo.ScheduleJobVO`

REST API 的輸入輸出傳輸物件，實作 `Serializable`（serialVersionUID=1L）。

| 欄位 | 型別 | 說明 |
|---|---|---|
| `jobName` | String | 排程唯一識別名稱（對應 model.Job.name） |
| `jobDescription` | String | 排程描述（對應 model.Job.description） |
| `timeUnit` | Integer | 時間單位碼（對應 ScheduleEnum.timeUnit） |
| `exeStatus` | String | 執行狀態描述（字串，未來擴充用） |
| `status` | Integer | 使用狀態 |
| `jobGroup` | String | 排程群組名稱（對應 model.Job.group） |
| `jobClass` | String | 排程 Job 的完整類別名稱（fully qualified，對應 model.Job.clazz） |
| `repeatInterval` | Integer | 固定間隔時間（配合 timeUnit 使用） |
| `cronExpression` | String | Cron 表示式（優先於 timeUnit+repeatInterval） |
| `message` | String | 錯誤或操作回饋訊息（由 BaseJob 在例外時設值） |
| `startTime` | LocalDateTime | 排程開始時間（格式：`yyyy-MM-dd HH:mm:ss`，時區 GMT+8） |
| `endTime` | LocalDateTime | 排程結束時間（同上） |
| `jobDataMap` | JobDataMap | 傳遞給 Job 的任意 key-value 參數 |

---

### 3.8 `Job`（model）

**完整路徑：** `com.zipe.quartz.model.Job`

排程領域模型，欄位名稱與 `ScheduleJobVO` 不同，由 `BaseJob.convertToJob()` 負責映射。

| VO 欄位 | Job 欄位 |
|---|---|
| `jobName` | `name` |
| `jobDescription` | `description` |
| `jobGroup` | `group` |
| `jobClass` | `clazz` |
| `jobDataMap` | `dataMap`（型別 JobDataMap） |

---

### 3.9 AutoConfiguration 類別

| 類別 | 職責 |
|---|---|
| `DataSourceAutoConfiguration` | JDBC 模式下建立 Quartz 專屬 HikariCP DataSource，條件：`spring.quartz.job-store-type=jdbc` |
| `InitialJobAutoConfiguration` | 讀取 `quartz-jobs.properties` 批次初始化排程，自動 Import `QuartzController`；條件：`spring.quartz.enable=true` |
| `QuartzDataSourceProperties` | 綁定 `spring.datasource.quartz.*` 屬性 |
| `QuartzJobPropertyConfig` | 綁定 `quartz.job-map.*` 批次排程定義 |

---

## 4. 核心協作流程

### 4.1 應用啟動：自動批次初始化排程

```
Spring Boot 啟動
  └─ 掃描 AutoConfiguration.imports
       ├─ InitialJobAutoConfiguration（ConditionalOnProperty spring.quartz.enable=true）
       │    ├─ @PropertySource 載入 quartz.properties / quartz-jobs.properties / spring-quartz.properties
       │    ├─ QuartzJobPropertyConfig 綁定 quartz.job-map.*
       │    ├─ @Import QuartzController → 註冊為 REST Controller Bean
       │    └─ @Bean createJobs()
       │         ├─ 1. scheduler.deleteJobs（group="schedule" 的所有 JobKey）
       │         ├─ 2. 遍歷 jobMap，建構 Job 物件（group 強制設為 "file"）
       │         ├─ 3. QuartzJobUtil.buildJobDetail() → JobDetail（Class.forName 動態載入）
       │         ├─ 4. QuartzJobUtil.buildJobTrigger(ScheduleEnum.CRON.setExpression(cron)) → CronTrigger
       │         └─ 5. scheduler.scheduleJob(detail, triggerSet, replace=true)
       │
       └─ DataSourceAutoConfiguration（僅 JDBC 模式，job-store-type=jdbc）
            ├─ @PropertySource 載入 quartz-datasource.properties（由消費方提供）
            ├─ QuartzDataSourceProperties 綁定 spring.datasource.quartz.*
            └─ @Bean quartzDataSource() → HikariDataSource（@Primary @QuartzDataSource）
```

### 4.2 REST API 動態操作：以 register 為例

```
HTTP POST /quartz/register
  Body: { "jobName":"ReportJob", "jobGroup":"report",
          "jobClass":"com.example.ReportJob",
          "cronExpression":"0 0 2 * * ?" }

  └─ QuartzController.registerJob(ScheduleJobVO)
       └─ BaseJob.mergeJobProcess(scheduleJobVO)
            └─ BaseJob.scheduleJobStatusProcess(vo, MERGE)
                 ├─ 判斷 cronExpression 不為空
                 │    └─ scheduleBuilder = ScheduleEnum.CRON.setExpression("0 0 2 * * ?")
                 ├─ convertToJob(vo) → Job 物件
                 └─ createJob(job, scheduleBuilder)
                      ├─ TriggerKey triggerKey = (jobName, jobGroup)
                      ├─ scheduler.getTrigger(triggerKey) → 已存在則 unscheduleJob
                      ├─ new QuartzJobUtil(job)
                      ├─ buildJobDetail(job) → Class.forName("com.example.ReportJob")
                      ├─ buildJobTrigger(scheduleBuilder) → CronTrigger
                      └─ scheduler.scheduleJob(jobDetail, {trigger}, replace=true)

  ← HTTP 200 OK：回傳原始 ScheduleJobVO（message 欄位於錯誤時被填入）
```

### 4.3 立即執行一次（run）

```
HTTP POST /quartz/run
  Body: { "jobName":"DataSync", "jobGroup":"batch",
          "jobClass":"com.example.DataSyncJob", "repeatInterval":1 }

  └─ QuartzController.run(vo)
       └─ BaseJob.runJobProcess(vo)
            └─ scheduleJobStatusProcess(vo, ONCE)
                 ├─ ScheduleBuilder = ScheduleEnum.NOW.setCycle(repeatInterval)
                 │    → SimpleScheduleBuilder.withMisfireHandlingInstructionFireNow()
                 ├─ vo.startTime = null（強制立即啟動）
                 └─ executeOnce(job, builder)
                      ├─ job.name = "DataSync-Once"（加後綴防衝突）
                      └─ createJob(job, builder)（同 register 流程）
```

### 4.4 業務 Job 執行（Quartz 觸發時）

```
Quartz Scheduler 觸發 Trigger
  └─ 依 JobDetail.jobClass 實例化 Job
       └─ QuartzJobBean.execute(JobExecutionContext)
            └─ QuartzJobFactory.executeInternal(context)   ← Template Method
                 ├─ beforeExecuteJobLog(context)  → log.info("排程名稱:{} 執行開始...")
                 ├─ try: executeJob(context)       ← 子類實作業務邏輯
                 │    └─ 可從 context.getJobDetail().getJobDataMap() 取參數
                 ├─ catch: errorExecuteJobLog(context, e) → log.error(...)
                 └─ afterExecuteJobLog(context)   → log.info("排程名稱:{} 執行結束...")
```

### 4.5 類別依賴關係圖

```
AutoConfiguration.imports
  ├─ DataSourceAutoConfiguration
  │    └─ uses: QuartzDataSourceProperties, HikariDataSource
  └─ InitialJobAutoConfiguration
       ├─ uses: QuartzJobPropertyConfig → Map<String, Job>
       ├─ uses: QuartzJobUtil（buildJobDetail, buildJobTrigger）
       ├─ uses: ScheduleEnum.CRON
       └─ imports: QuartzController

QuartzController extends BaseJob
  └─ BaseJob
       ├─ uses: Scheduler（Quartz）
       ├─ uses: QuartzJobUtil（每次呼叫 new 一個實例）
       ├─ uses: ScheduleEnum（getTimeUnit, CRON, NOW）
       ├─ uses: ScheduleJobStatusEnum（switch 分派）
       ├─ uses: Job（領域模型，透過 convertToJob 建立）
       └─ uses: ScheduleJobVO（輸入/輸出）

QuartzJobFactory extends QuartzJobBean
  └─ abstract executeJob(JobExecutionContext)
       └─ 由業務 Job 實作（TestJob、自訂 Job...）
```

---

## 5. 自動配置運作原理

### 5.1 SPI 入口

Spring Boot 3.x 啟動時掃描所有 jar 中的以下檔案：

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

本模組登錄兩個 AutoConfiguration 類別：

```
com.zipe.quartz.autoconfiguration.DataSourceAutoConfiguration
com.zipe.quartz.autoconfiguration.InitialJobAutoConfiguration
```

### 5.2 `DataSourceAutoConfiguration` 條件與 Bean 註冊

```java
@AutoConfiguration
@ConditionalOnClass(QuartzDataSourceProperties.class)           // classpath 含此類才啟用
@EnableConfigurationProperties(QuartzDataSourceProperties.class)
@PropertySource("classpath:quartz-datasource.properties")       // 由消費方 classpath 提供
@ConditionalOnExpression(
  "${spring.quartz.enable:true} && " +
  "'${spring.quartz.job-store-type}'.equals('jdbc')"
)                                                               // 同時需 enable=true 且 JDBC 模式
```

注冊的 Bean：

```java
@Primary
@Bean("quartzDataSource")
@ConfigurationProperties(prefix = "spring.datasource.hikari")  // HikariCP pool 設定
@QuartzDataSource                                              // 讓 Spring Boot Quartz 整合識別
public DataSource quartzDataSource() { ... }
```

屬性綁定：

| 屬性鍵 | 說明 |
|---|---|
| `spring.datasource.quartz.url` | JDBC URL |
| `spring.datasource.quartz.username` | 帳號 |
| `spring.datasource.quartz.password` | 密碼 |
| `spring.datasource.quartz.driver-class-name` | 驅動類別 |
| `spring.datasource.hikari.*` | 連線池細節（由 hikari-config.properties 設預設值） |

:::important `quartz-datasource.properties` 由消費方提供
`DataSourceAutoConfiguration` 透過 `@PropertySource("classpath:quartz-datasource.properties")` 載入資料來源設定。此檔案**不在** starter 內建，需由消費方在自己的 `src/main/resources/` 下提供（範例見[配置參考](./configuration.md)）。若未提供且 job-store-type=jdbc，屬性值為空，DataSource 建立會失敗。
:::

### 5.3 `InitialJobAutoConfiguration` 條件與 Bean 註冊

```java
@AutoConfiguration
@EnableScheduling                                            // 啟用 Spring @Scheduled 支援
@ConditionalOnClass(QuartzJobPropertyConfig.class)
@EnableConfigurationProperties(QuartzJobPropertyConfig.class)
@Import(QuartzController.class)                              // 自動注冊 QuartzController Bean
@PropertySource({
  "classpath:quartz.properties",                            // Quartz 原生 jobStore/threadPool
  "classpath:quartz-jobs.properties",                       // 批次排程定義
  "classpath:spring-quartz.properties"                      // Spring Boot Quartz 整合設定
})
@ConditionalOnProperty(name = "spring.quartz.enable", havingValue = "true")
```

`createJobs()` Bean 方法邏輯：

1. 讀取 `quartz.job-map.*`（`QuartzJobPropertyConfig.jobMap`）
2. 先清空 group=`"schedule"` 的所有 Job
3. 以 `QuartzJobUtil` 動態建構 `JobDetail`（storeDurably）與 `CronTrigger`
4. `scheduler.scheduleJob(detail, triggerSet, replace=true)` 寫入 Scheduler

`quartz-jobs.properties` 設定格式：

```properties
quartz.job-map[MyJob].name=MyJob
quartz.job-map[MyJob].description=說明文字
quartz.job-map[MyJob].group=schedule
quartz.job-map[MyJob].clazz=com.example.job.MyJob
quartz.job-map[MyJob].cronExpression=0/15 * * * * ? *
```

`spring-quartz.properties` 主要設定：

```properties
spring.quartz.auto-startup=true
spring.quartz.overwrite-existing-jobs=false
spring.quartz.scheduler-name=jobScheduler
spring.quartz.startup-delay=0
spring.quartz.wait-for-jobs-to-complete-on-shutdown=false
spring.quartz.jdbc.initialize-schema=always
```

`quartz.properties`（JDBC 模式，Quartz 原生設定）：

```properties
org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
org.quartz.jobStore.tablePrefix=QRTZ_
org.quartz.jobStore.dataSource=quartzDataSource
org.quartz.threadPool.threadCount=15
org.quartz.threadPool.threadPriority=5
```

---

## 6. 開發擴充指南

### 情境 A：新增一個業務排程 Job（最常見需求）

**步驟 1：** 建立業務 Job 類別，繼承 `QuartzJobFactory`（不是 `BaseJob`）：

```java
package com.example.job;

import com.zipe.quartz.job.QuartzJobFactory;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

@Slf4j
public class DailyReportJob extends QuartzJobFactory {

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        // 從 JobDataMap 取參數
        String reportType = context.getJobDetail()
            .getJobDataMap().getString("reportType");
        log.info("產生報表，類型：{}", reportType);
        // 業務邏輯...
    }
}
```

**步驟 2（選擇性）：若要在啟動時自動執行，** 在 `quartz-jobs.properties` 加入：

```properties
quartz.job-map[DailyReport].name=DailyReport
quartz.job-map[DailyReport].description=每日報表
quartz.job-map[DailyReport].group=schedule
quartz.job-map[DailyReport].clazz=com.example.job.DailyReportJob
quartz.job-map[DailyReport].cronExpression=0 0 2 * * ?
```

**步驟 3（選擇性）：若要透過 REST API 動態註冊，** POST `/quartz/register`：

```bash
curl -X POST http://localhost:8080/quartz/register \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "DailyReport",
    "jobGroup": "report",
    "jobClass": "com.example.job.DailyReportJob",
    "cronExpression": "0 0 2 * * ?",
    "jobDataMap": { "reportType": "PDF" }
  }'
```

**不需修改的檔案：** `ScheduleEnum`、`BaseJob`、`QuartzController`、任何 AutoConfiguration。

---

### 情境 B：讓 Job 能注入 Spring Bean

Quartz 預設使用 `SimpleJobFactory` 反射建立 Job 實例，**不經過 Spring 容器**，因此 `@Autowired` 欄位不會被注入。需要在消費方設定 `SpringBeanJobFactory`：

```java
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(
            ApplicationContext context) {
        return schedulerFactoryBean -> {
            SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
            jobFactory.setApplicationContext(context);
            schedulerFactoryBean.setJobFactory(jobFactory);
        };
    }
}
```

完成後，Job 類別上的 `@Autowired` 欄位即可注入 Spring Bean：

```java
@Slf4j
public class DailyReportJob extends QuartzJobFactory {

    @Autowired
    private ReportService reportService;   // 配置 SpringBeanJobFactory 後才能注入

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        reportService.generateDailyReport();
    }
}
```

---

### 情境 C：切換為 JDBC 儲存模式（生產環境）

**步驟 1：** 在消費方 `src/main/resources/` 提供 `quartz-datasource.properties`：

```properties
spring.datasource.quartz.username=quartz_user
spring.datasource.quartz.password=your_password
spring.datasource.quartz.url=jdbc:mysql://localhost:3306/quartz_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Taipei
spring.datasource.quartz.driver-class-name=com.mysql.cj.jdbc.Driver
```

**步驟 2：** 在 `application.yml` 設定：

```yaml
spring:
  quartz:
    enable: true
    job-store-type: jdbc
    jdbc:
      initialize-schema: always   # 首次啟動建表；建好後務必改為 never
```

**步驟 3：** 加入 MySQL 驅動依賴（消費方 pom.xml）：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

`DataSourceAutoConfiguration` 的 `@ConditionalOnExpression` 會自動偵測 `job-store-type=jdbc` 而啟用 Quartz 專屬 DataSource。

:::danger 生產環境必須將 initialize-schema 改為 never
`spring.quartz.jdbc.initialize-schema=always` 會在每次啟動時重建 `QRTZ_*` 資料表，清除所有持久化的排程資料。正式環境首次建表後，務必將此設定改為 `never`。
:::

---

### 情境 D：新增時間單位（例如支援毫秒間隔）

修改 `ScheduleEnum.java`，新增常數：

```java
MILLISECOND(9) {
    @Override
    public ScheduleBuilder setCycle(int interval) {
        return SimpleScheduleBuilder.simpleSchedule()
            .withIntervalInMilliseconds(interval)
            .repeatForever();
    }
},
```

無需修改其他類別，`BaseJob.scheduleJobStatusProcess()` 的 MERGE 分支呼叫 `ScheduleEnum.getTimeUnit()` 自動包含新 enum。

---

### 情境 E：新增 REST API 端點（例如查詢排程清單）

目前 `QuartzController` 缺少 GET /quartz/list，可直接擴充：

```java
@GetMapping("/list")
public ResponseEntity<List<Map<String, Object>>> listJobs() throws SchedulerException {
    List<Map<String, Object>> result = new ArrayList<>();
    for (String group : scheduler.getJobGroupNames()) {
        for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group))) {
            JobDetail detail = scheduler.getJobDetail(jobKey);
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            result.add(Map.of(
                "name", jobKey.getName(),
                "group", jobKey.getGroup(),
                "description", detail.getDescription()
            ));
        }
    }
    return ResponseEntity.ok(result);
}
```

---

### 情境 F：叢集部署（多節點）

記憶體模式（RAMJobStore）不支援叢集，各節點會各自獨立執行同一排程。JDBC 模式需在消費方提供的 `quartz.properties` 中追加叢集設定：

```properties
org.quartz.jobStore.isClustered=true
org.quartz.jobStore.clusterCheckinInterval=15000
org.quartz.scheduler.instanceId=AUTO
org.quartz.scheduler.instanceName=ClusteredScheduler
```

---

## 7. 維護注意事項與常見陷阱

### 陷阱 1：`InitialJobAutoConfiguration` 的 group 不一致

**現象：** `createJobs()` 在刪除階段使用 `GroupMatcher.jobGroupEquals("schedule")`，但建立 Job 時 group 固定設為 `"file"`（hardcoded 常數）。

**影響：** properties 中設定的 `group=schedule` 欄位值實際上被忽略。重啟時刪除的是 group=`"schedule"` 的 Job，但實際排程在 `"file"` group，舊版本的 `"file"` group 排程永遠不會被清理，可能累積殭屍排程。

**處置建議：** 生產環境切換 JDBC 模式後，定期檢查 `QRTZ_JOB_DETAILS` 資料表中 group=`"file"` 的排程是否與預期一致。

---

### 陷阱 2：錯誤處理不回傳 HTTP 錯誤狀態碼

`BaseJob.scheduleJobStatusProcess()` 的 catch 區塊只記錄 log 並設定 `scheduleJobDetail.message`，不回傳 HTTP 4xx/5xx。REST API 呼叫方永遠收到 **HTTP 200**，需自行檢查回應 body 的 `message` 欄位判斷是否失敗：

```bash
# 範例回應（雖為 200，但 message 不為空代表失敗）
{
  "jobName": "NonExistentJob",
  "message": "No class found for name: com.example.NonExistentJob"
}
```

---

### 陷阱 3：`HelloWorldJob` 模組內建的 @Scheduled Job

`HelloWorldJob` 使用 `@Scheduled(cron="0/20 * * * * ?")` 並標記 `@Component`，與整個模組的 Quartz 架構無關，純靠 `@EnableScheduling` 運作。引入此 starter 後，**每 20 秒會自動打一筆 log**，在正式環境可能造成噪音。

**處置建議：** 確認這個內建 Job 是否影響正式環境，必要時在消費方覆寫或排除此 Bean。

---

### 陷阱 4：`spring.quartz.jdbc.initialize-schema=always` 的生產風險

`spring-quartz.properties` 預設值為 `always`，在 JDBC 模式下每次啟動都會重建 `QRTZ_*` 資料表，**清除所有持久化排程**。務必在正式環境覆寫為 `never`：

```yaml
spring:
  quartz:
    jdbc:
      initialize-schema: never
```

---

### 陷阱 5：`executeOnce` 的命名衝突

`runJobProcess()` 在 jobName 後附加 `-Once` 後綴再建立排程。若多次快速呼叫同一 job 的 run，會因 triggerKey 相同而進入 unschedule + reschedule 邏輯，導致前一次「一次性」排程被取消。高並發場景需注意。

---

### 陷阱 6：`ScheduleJobVO.jobDataMap` 的序列化限制

`ScheduleJobVO.jobDataMap` 型別為 Quartz `JobDataMap`（繼承 `HashMap`）。若 value 為複雜物件（非基本型別），在 JDBC 模式下需確認 `org.quartz.jobStore.useProperties=false`（允許序列化物件），否則建議只傳字串型別值以確保相容性。

---

### 陷阱 7：`Class.forName()` 與 ClassLoader

`QuartzJobUtil.buildJobDetail()` 使用 `Class.forName(job.getClazz())` 不帶 ClassLoader 參數。在 Spring Boot fat-jar 環境通常無問題，但若類別名稱錯誤或 classpath 不包含該類別，會拋出 `ClassNotFoundException`，且此例外會被 `BaseJob` 的 catch 區塊攔截並設入 `message` 欄位（見陷阱 2）。

---

### 陷阱 8：關閉時不等待 Job 完成

`spring-quartz.properties` 預設 `wait-for-jobs-to-complete-on-shutdown=false`，應用關閉時不等待正在執行的 Job 完成，可能造成 Job 中途被強制中斷。若業務 Job 有資料完整性要求，建議消費方覆寫為 `true`：

```yaml
spring:
  quartz:
    wait-for-jobs-to-complete-on-shutdown: true
```
