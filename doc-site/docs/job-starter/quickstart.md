---
id: quickstart
title: 快速開始
sidebar_position: 2
---

# 快速開始

本頁帶您撰寫一個簡單的排程任務，並透過 REST API 或 properties 檔案自動啟用。

## 前置需求

- JDK 17 以上、Spring Boot 4.0.x。
- 已將 `job-spring-boot-starter` 安裝至本地 Maven Repository。
- 若使用 JDBC 模式，需準備一組資料庫並建立 Quartz 資料表。

:::note
初次嘗試建議先使用記憶體（RAM）模式，省去建表步驟，待邏輯驗證無誤後再切換為 JDBC 模式。
:::

## Step 1：安裝模組

```bash
cd job-spring-boot-starter
./mvnw clean install -DskipTests
```

## Step 2：加入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>job-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

## Step 3：設定 application.yml

以記憶體模式啟用排程功能：

```yaml
spring:
  quartz:
    enable: true
    job-store-type: memory
    auto-startup: true
    overwrite-existing-jobs: false
    scheduler-name: jobScheduler
```

:::important 設定命名空間
模組讀取的是 `spring.quartz.*` 命名空間，而非 `zipe.quartz.*`。
- 是否啟用排程功能：`spring.quartz.enable`（預設 `true`）
- 儲存模式：`spring.quartz.job-store-type`（`memory` 或 `jdbc`）
:::

## Step 4：撰寫業務 Job

業務 Job 繼承 `QuartzJobFactory` 並覆寫 `executeJob()` 方法。`QuartzJobFactory` 會自動提供 before/after/error 日誌，您只需專注在業務邏輯：

```java
package com.example.job;

import com.zipe.quartz.job.QuartzJobFactory;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

@Slf4j
public class ReportJob extends QuartzJobFactory {

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        // 從 JobDataMap 取得傳入參數（可選）
        String reportType = context.getJobDetail()
            .getJobDataMap().getString("reportType");
        log.info("產生報表中，類型：{}，時間：{}", reportType, java.time.LocalDateTime.now());
        // 撰寫實際業務邏輯
    }
}
```

:::warning 繼承的類別是 `QuartzJobFactory`，不是 `BaseJob`
- **`QuartzJobFactory`**：排程被 Quartz 觸發時的執行框架，業務 Job **繼承此類**。
- **`BaseJob`**：排程生命週期管理（新增、暫停、刪除等），`QuartzController` 繼承此類；業務 Job **不應繼承**。
:::

## Step 5：透過 REST API 動態註冊排程

啟動應用程式後，使用 POST `/quartz/register` 以 JSON body 傳入排程定義：

```bash
# 以 Cron 表示式每天凌晨 2 點執行
curl -X POST http://localhost:8080/quartz/register \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "ReportJob",
    "jobGroup": "report",
    "jobClass": "com.example.job.ReportJob",
    "cronExpression": "0 0 2 * * ?",
    "jobDataMap": { "reportType": "PDF" }
  }'

# 以固定間隔（每 30 秒）執行
curl -X POST http://localhost:8080/quartz/register \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "ReportJob",
    "jobGroup": "report",
    "jobClass": "com.example.job.ReportJob",
    "timeUnit": 1,
    "repeatInterval": 30
  }'
```

`timeUnit` 對應關係：`1`=秒、`2`=分、`3`=時、`4`=天、`5`=週、`6`=月、`7`=年。

:::tip Cron 優先於 timeUnit
當 `cronExpression` 欄位不為空時，`timeUnit` 與 `repeatInterval` 會被忽略，排程以 Cron 表示式為準。
:::

## Step 6（選擇性）：透過 properties 在啟動時自動建立排程

若要在應用啟動時自動批次建立固定排程，在 `src/main/resources/quartz-jobs.properties` 加入定義：

```properties
quartz.job-map[ReportJob].name=ReportJob
quartz.job-map[ReportJob].description=每日報表
quartz.job-map[ReportJob].group=schedule
quartz.job-map[ReportJob].clazz=com.example.job.ReportJob
quartz.job-map[ReportJob].cronExpression=0 0 2 * * ?
```

應用啟動時，`InitialJobAutoConfiguration` 會自動讀取此檔案並向 Scheduler 批次註冊排程。

:::note `quartz-jobs.properties` 的 group 欄位
`group` 欄位雖可填入任意值（如 `schedule`），但 `InitialJobAutoConfiguration` 建立排程時 group 固定為 `"file"`（hardcoded），`group` 欄位值目前未被實際使用。這是一個已知的設計不一致，在使用時請留意。
:::

## Step 7：立即觸發排程驗證

```bash
# 立即執行一次（不影響常駐排程）
curl -X POST http://localhost:8080/quartz/run \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "ReportJob",
    "jobGroup": "report",
    "jobClass": "com.example.job.ReportJob"
  }'
```

啟動應用程式後觀察主控台，應看到 `QuartzJobFactory` 輸出的排程開始/結束 log：

```bash
./mvnw spring-boot:run
```

:::tip Cron 表示式格式
Quartz 的 Cron 為 **6 至 7 欄位**（秒 分 時 日 月 週 [年]），與 Linux crontab 的 5 欄位不同，請特別留意。例如 `0 0 2 * * ?` 代表「每天凌晨 2 點整」。
:::

:::warning `jobClass` 必須填入完整類別名稱
`jobClass`（或 `clazz`）必須填入完整的套件路徑名稱（如 `com.example.job.ReportJob`），模組使用 `Class.forName()` 動態載入，名稱錯誤會導致 `ClassNotFoundException`。
:::

:::note 讓 Job 能注入 Spring Bean
Quartz 預設不透過 Spring 容器建立 Job 實例，因此 `@Autowired` 欄位不會被注入。若業務 Job 需要注入 Service 或 Repository，需要額外在消費方設定 `SpringBeanJobFactory`。詳見[架構與開發指南](./architecture.md#情境-b讓-job-能注入-spring-bean)。
:::
