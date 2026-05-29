---
id: scenario-job
title: 情境三：排程任務應用
sidebar_position: 4
description: 使用 base + job Starter 建構 Quartz 排程系統
---

# 情境三：排程任務應用

## 情境說明

本情境示範如何建構一個**需要定時執行任務的系統**，例如：每日報表產生、定期資料同步、批次清理、定時通知等。`job-spring-boot-starter` 以 Quartz 為核心，提供四種不同的排程模式，並支援透過 REST API 在執行期間動態新增、暫停、恢復與刪除排程。

## 使用的 Starters

| Starter | 在本情境的角色 |
|---|---|
| `base-spring-boot-starter` | 提供工具類等基礎設施 |
| `job-spring-boot-starter` | 提供 Quartz Scheduler 自動配置、排程管理 API |
| `db-spring-boot-starter`（可選） | 僅在排程需持久化（`job-store-type: jdbc`）時搭配使用 |

## pom.xml 依賴配置

```xml
<dependencies>
    <!-- 提供 REST API 給 QuartzController -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>com.zipe</groupId>
        <artifactId>base-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
    <dependency>
        <groupId>com.zipe</groupId>
        <artifactId>job-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>

    <!-- memory 模式不需 DB 驅動；jdbc 模式需對應驅動（以 H2 為例） -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

## application.yml 設定

記憶體模式（最簡單，不需資料庫）：

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  quartz:
    enable: true
    job-store-type: memory
```

JDBC 模式（排程持久化）：

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  quartz:
    enable: true
    job-store-type: jdbc
    jdbc:
      initialize-schema: always
```

:::warning allow-bean-definition-overriding 必須開啟
`allow-bean-definition-overriding: true` 必須設定，否則 `job-starter` 的 DataSource Bean 可能與應用本身的 DataSource 發生衝突。
:::

## quartz-datasource.properties 設定

僅在 JDBC 模式（`job-store-type: jdbc`）下才會被載入（透過 `@ConditionalOnExpression`），用於指定 Quartz 排程器的獨立資料來源：

```properties
spring.datasource.quartz.username=user2
spring.datasource.quartz.password=example2
spring.datasource.quartz.url=jdbc:mysql://localhost:3306/example2?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC
spring.datasource.quartz.driver-class-name=com.mysql.cj.jdbc.Driver
```

`job-starter` 的 `DataSourceAutoConfiguration` 會以此建立 HikariCP 連線池並標記為 `@QuartzDataSource`，供 Spring Boot Quartz 自動配置使用。

:::note 記憶體模式仍需此檔案存在
即使使用 memory 模式，由於 `@PropertySource` 有引用此檔案，classpath 下必須存在 `quartz-datasource.properties`（可為內容無效但檔案存在），否則啟動會報錯。
:::

## 四種排程模式對比

| 模式 | 設定來源 | 實作方式 | 持久化 | 適用場景 |
|---|---|---|---|---|
| 基本（Properties） | `quartz-jobs.properties` | 繼承 `QuartzJobFactory` | 否（memory） | 啟動即固定的排程 |
| Annotation | `@Scheduled` | POJO + `@Component` | 否 | 最輕量、固定週期 |
| DB（資料庫） | REST API 動態註冊 | 繼承 `QuartzJobFactory` | 是（jdbc） | 需動態管理、叢集 |
| XML（Properties） | `quartz-jobs.properties` | 繼承 `QuartzJobFactory` | 否 | 啟動即固定的排程 |

## 各模式完整程式碼範例

### Annotation 模式（使用 Spring 原生 @Scheduled）

最輕量，與 Quartz 無關，由 `@EnableScheduling` 驅動（`InitialJobAutoConfiguration` 已標記）：

```java
@Slf4j
@Component
public class ExampleAnnotationJob {

    @Scheduled(cron = "0/15 * * * * ? *")
    public void example() {
        log.info("執行 ExampleAnnotationJob");
    }
}
```

### Properties 設定模式（ExampleXmlJob）

繼承 `QuartzJobFactory`，override `executeJob`，無須 `@Component`：

```java
@Slf4j
public class ExampleXmlJob extends QuartzJobFactory {

    @Override
    protected void executeJob(JobExecutionContext context) {
        log.info("執行 ExampleXmlJob");
    }
}
```

`QuartzJobFactory` 繼承鏈為 `QuartzJobFactory → QuartzJobBean（Spring）→ Job（Quartz）`。其 `executeInternal()` 會依序呼叫 `beforeExecuteJobLog()`（記錄開始）、`executeJob()`（子類業務邏輯）、`afterExecuteJobLog()`（記錄結束，錯誤時改呼叫 `errorExecuteJobLog()`）。

### DB 資料庫模式（ExampleDbJob）

結構與 `ExampleXmlJob` 完全相同，差異在於排程定義存放於資料庫，並透過 REST API 動態註冊。可從 `JobExecutionContext` 取得動態參數：

```java
@Slf4j
public class ExampleDbJob extends QuartzJobFactory {

    @Override
    protected void executeJob(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        log.info("執行 ExampleDbJob，參數：{}", dataMap);
    }
}
```

## quartz-jobs.properties 設定格式

靜態排程定義放在 `quartz-jobs.properties`，對應 `QuartzJobPropertyConfig` 的 `Map<String, Job> jobMap`：

```properties
# [ExampleJob] 為 Map 的 key（純識別碼，與 class 名稱無關）
quartz.job-map[ExampleJob].name=ExampleXmlJob
quartz.job-map[ExampleJob].description=每15秒執行一次
quartz.job-map[ExampleJob].group=schedule
quartz.job-map[ExampleJob].clazz=com.example.job.ExampleXmlJob
quartz.job-map[ExampleJob].cronExpression=0/15 * * * * ? *
```

- `[ExampleJob]` 只是 Map 的 key（設定識別碼）；`.name` 才是真正的 JobDetail name（用於 `JobKey`）。
- `.clazz` 指向實際執行的 Java class 全名，啟動時以 `Class.forName()` 動態載入。
- `InitialJobAutoConfiguration` 啟動時會先刪除所有 group=`schedule` 的 Job，再重建（重建時 group 強制設為 `"file"`）。

## 動態管理排程的 API

`job-starter` 提供 `QuartzController`（路徑 `/quartz`），所有操作以 POST JSON 呼叫，請求體為 `ScheduleJobVO`：

```bash
# 新增或更新排程
curl -X POST http://localhost:8080/quartz/register \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "MyJob",
    "jobGroup": "myGroup",
    "jobClass": "com.example.job.ExampleDbJob",
    "cronExpression": "0/30 * * * * ? *",
    "jobDescription": "每30秒執行"
  }'
```

| 端點 | 用途 |
|---|---|
| `POST /quartz/register` | 新增或更新排程（已存在則先移除再重建） |
| `POST /quartz/pause` | 暫停排程 |
| `POST /quartz/resume` | 恢復排程 |
| `POST /quartz/delete` | 刪除排程 |
| `POST /quartz/run` | 立即執行一次（jobName 加 `-Once` 後綴） |

`暫停`、`恢復`、`刪除`只需傳入 `jobName` 與 `jobGroup`：

```json
{"jobName": "MyJob", "jobGroup": "myGroup"}
```

`ScheduleEnum.timeUnit` 對照：0=立即(NOW)、1=秒、2=分、3=時、4=天、5=週、6=月、7=年。若 `cronExpression` 為空，則依 `timeUnit` + `repeatInterval` 建立固定週期觸發器。

:::tip 各模式的適用場景
- **Annotation 模式**：排程固定、不需動態調整時最簡單。其實只要用 Spring Boot 原生 `@EnableScheduling` 即可，不一定需要 `job-starter`。
- **Properties 模式**：排程在啟動時即確定、希望以設定檔集中管理時適用。
- **DB 模式**：需要在執行期間動態增刪排程、或需要叢集（多節點共用排程狀態）時，才使用 `job-store-type: jdbc` 搭配資料庫持久化。
:::
