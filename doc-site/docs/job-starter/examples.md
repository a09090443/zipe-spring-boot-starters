---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁示範 `job-spring-boot-starter` 的排程撰寫、動態管理與 REST API 操作。所有範例均基於實際類別介面，可直接參考實作。

---

## 基礎使用範例

### 撰寫一個排程任務

業務 Job 繼承 `QuartzJobFactory` 並覆寫 `executeJob()`。框架會自動記錄 before/after/error 日誌，開發者只需專注在業務邏輯。

```java
package com.example.job;

import com.zipe.quartz.job.QuartzJobFactory;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

@Slf4j
public class CleanupJob extends QuartzJobFactory {

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        // 從 JobDataMap 取得傳入參數（可選）
        String targetTable = context.getJobDetail()
            .getJobDataMap().getString("targetTable");
        log.info("開始清除過期資料，目標資料表：{}", targetTable);
        // 業務邏輯...
    }
}
```

:::warning 繼承 `QuartzJobFactory`，不是 `BaseJob`
業務 Job 應繼承 `QuartzJobFactory` 並覆寫 `executeJob()`。`BaseJob` 負責排程生命週期管理，業務 Job 不應繼承它。
:::

---

### 讓 Job 能注入 Spring Bean

Quartz 預設不透過 Spring 容器建立 Job 實例，需先在消費方設定 `SpringBeanJobFactory`，`@Autowired` 欄位才能被注入。

**步驟 1：在設定類加入 `SchedulerFactoryBeanCustomizer`**

```java
package com.example.config;

import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

**步驟 2：Job 類別即可使用 `@Autowired`**

```java
package com.example.job;

import com.example.service.CleanupService;
import com.zipe.quartz.job.QuartzJobFactory;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class CleanupJob extends QuartzJobFactory {

    @Autowired
    private CleanupService cleanupService;   // 設定 SpringBeanJobFactory 後才能注入

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        int removed = cleanupService.removeExpiredData();
        log.info("清除過期資料 {} 筆", removed);
    }
}
```

---

## 進階使用範例

### 透過 REST API 動態管理排程生命週期

`QuartzController` 提供五個 POST 端點，均以 JSON body 傳入 `ScheduleJobVO`。以下為各操作的完整 curl 範例：

**新增/覆蓋排程（register）**

以 Cron 表示式建立每天凌晨 2 點執行的排程：

```bash
curl -X POST http://localhost:8080/quartz/register \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "CleanupJob",
    "jobGroup": "maintenance",
    "jobClass": "com.example.job.CleanupJob",
    "cronExpression": "0 0 2 * * ?",
    "jobDataMap": { "targetTable": "expired_records" }
  }'
```

以固定間隔（每 30 秒）執行，使用 `timeUnit`（`1`=秒）與 `repeatInterval`：

```bash
curl -X POST http://localhost:8080/quartz/register \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "CleanupJob",
    "jobGroup": "maintenance",
    "jobClass": "com.example.job.CleanupJob",
    "timeUnit": 1,
    "repeatInterval": 30
  }'
```

**暫停排程（pause）**

```bash
curl -X POST http://localhost:8080/quartz/pause \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "CleanupJob",
    "jobGroup": "maintenance"
  }'
```

**恢復排程（resume）**

```bash
curl -X POST http://localhost:8080/quartz/resume \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "CleanupJob",
    "jobGroup": "maintenance"
  }'
```

**刪除排程（delete）**

```bash
curl -X POST http://localhost:8080/quartz/delete \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "CleanupJob",
    "jobGroup": "maintenance"
  }'
```

**立即執行一次（run）**

立即觸發，不影響原有常駐排程（內部以 `CleanupJob-Once` 作為臨時排程名稱）：

```bash
curl -X POST http://localhost:8080/quartz/run \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "CleanupJob",
    "jobGroup": "maintenance",
    "jobClass": "com.example.job.CleanupJob",
    "jobDataMap": { "targetTable": "expired_records" }
  }'
```

:::note REST API 的錯誤回應
所有端點在操作失敗時**仍回傳 HTTP 200**，錯誤訊息會寫入回應 body 的 `message` 欄位。呼叫端需自行檢查此欄位以判斷是否成功。

```json
{
  "jobName": "CleanupJob",
  "jobGroup": "maintenance",
  "message": "No class found for name: com.example.job.CleanupJob"
}
```
:::

---

### 以 `BaseJob` 建立自訂管理服務

若需在程式碼中動態管理排程（而非透過 HTTP），可建立繼承 `BaseJob` 的自訂服務類別，呼叫 `protected` 方法：

```java
package com.example.service;

import com.zipe.quartz.base.BaseJob;
import com.zipe.quartz.vo.ScheduleJobVO;
import org.quartz.Scheduler;
import org.springframework.stereotype.Service;

/**
 * 繼承 BaseJob，在程式碼中操作排程生命週期。
 * BaseJob 的 protected 方法（mergeJobProcess 等）供子類直接呼叫。
 */
@Service
public class ScheduleManagementService extends BaseJob {

    public ScheduleManagementService(Scheduler scheduler) {
        super(scheduler);
    }

    public void registerCleanupJob(String cronExpression) {
        ScheduleJobVO vo = new ScheduleJobVO();
        vo.setJobName("CleanupJob");
        vo.setJobGroup("maintenance");
        vo.setJobClass("com.example.job.CleanupJob");
        vo.setCronExpression(cronExpression);
        mergeJobProcess(vo);   // upsert 語義：不存在則新增，已存在則覆蓋
    }

    public void pauseCleanupJob() {
        ScheduleJobVO vo = new ScheduleJobVO();
        vo.setJobName("CleanupJob");
        vo.setJobGroup("maintenance");
        pauseJobProcess(vo);
    }

    public void runCleanupJobNow() {
        ScheduleJobVO vo = new ScheduleJobVO();
        vo.setJobName("CleanupJob");
        vo.setJobGroup("maintenance");
        vo.setJobClass("com.example.job.CleanupJob");
        runJobProcess(vo);   // 立即執行一次，jobName 加 "-Once" 後綴
    }
}
```

---

## 常見情境

### 情境一：應用啟動時自動批次建立排程

在 `src/main/resources/quartz-jobs.properties` 定義排程，`InitialJobAutoConfiguration` 會在啟動時自動批次建立：

```properties
# quartz-jobs.properties（放在消費方 src/main/resources/）
quartz.job-map[CleanupJob].name=CleanupJob
quartz.job-map[CleanupJob].description=每日清除過期資料
quartz.job-map[CleanupJob].group=schedule
quartz.job-map[CleanupJob].clazz=com.example.job.CleanupJob
quartz.job-map[CleanupJob].cronExpression=0 0 2 * * ?

quartz.job-map[DataSync].name=DataSync
quartz.job-map[DataSync].description=每 15 分鐘同步資料
quartz.job-map[DataSync].group=schedule
quartz.job-map[DataSync].clazz=com.example.job.DataSyncJob
quartz.job-map[DataSync].cronExpression=0 0/15 * * * ?
```

每次應用啟動時，模組會先刪除 group=`"schedule"` 的舊排程，再依 properties 全量重建，確保排程定義與設定檔一致。

---

### 情境二：透過 `@EventListener` 在啟動後動態掛載排程

若排程需在應用完全就緒後才建立（例如需先讀取資料庫設定），可使用 `ApplicationReadyEvent`：

```java
package com.example.initializer;

import com.example.service.ScheduleManagementService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 應用完全啟動後，從資料庫讀取排程設定並動態掛載。
 */
@Component
public class JobInitializer {

    private final ScheduleManagementService scheduleService;

    public JobInitializer(ScheduleManagementService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initJobs() {
        // 從 DB 或其他來源讀取 Cron 設定
        String cronExpression = "0 0 3 * * ?";  // 範例：每天凌晨 3 點
        scheduleService.registerCleanupJob(cronExpression);
    }
}
```

---

### 情境三：排程任務存取資料庫

配合 `SpringBeanJobFactory` 設定後，排程任務可透過 `@Autowired` 注入 Repository 或 Service 存取資料庫，與一般業務邏輯撰寫方式相同：

```java
@Slf4j
public class DataSyncJob extends QuartzJobFactory {

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        log.info("開始資料同步...");
        int syncCount = dataSyncService.syncFromRemote();
        syncRecordRepository.save(new SyncRecord(syncCount, LocalDateTime.now()));
        log.info("資料同步完成，共同步 {} 筆", syncCount);
    }
}
```

---

### 情境四：`ScheduleJobVO` 欄位與 `timeUnit` 對應

以下整理 `ScheduleJobVO` 的完整欄位說明與 `timeUnit` 對應表，供 REST API 呼叫端參考：

**`timeUnit` 對應表**

| timeUnit 值 | 時間單位 | 觸發器類型 |
|---|---|---|
| `1` | 秒 | SimpleScheduleBuilder |
| `2` | 分鐘 | SimpleScheduleBuilder |
| `3` | 小時 | SimpleScheduleBuilder |
| `4` | 天 | CalendarIntervalScheduleBuilder |
| `5` | 週 | CalendarIntervalScheduleBuilder |
| `6` | 月 | CalendarIntervalScheduleBuilder |
| `7` | 年 | CalendarIntervalScheduleBuilder |

**含時間範圍的排程範例**

指定在 2025-01-01 08:00:00 至 2025-12-31 23:59:59 之間，每 5 分鐘執行一次：

```bash
curl -X POST http://localhost:8080/quartz/register \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "TimedReport",
    "jobGroup": "report",
    "jobClass": "com.example.job.TimedReportJob",
    "timeUnit": 2,
    "repeatInterval": 5,
    "startTime": "2025-01-01 08:00:00",
    "endTime": "2025-12-31 23:59:59"
  }'
```

---

## 常見問題

- **排程沒有觸發**：確認 `spring.quartz.enable=true` 且 `spring.quartz.auto-startup=true`；並確認 Cron 表示式正確（Quartz 為 6 至 7 欄位）。
- **重啟後排程消失**：記憶體模式（`job-store-type: memory`）不會持久化，請改用 JDBC 模式。
- **JDBC 模式啟動失敗**：通常是未提供 `quartz-datasource.properties` 或未建立 `QRTZ_*` 資料表，請確認兩者都已到位。
- **Job 中無法注入 Bean**：需配置 `SpringBeanJobFactory`，詳見上方「讓 Job 能注入 Spring Bean」範例。
- **REST API 回應 200 但操作無效**：檢查回應 body 的 `message` 欄位，所有操作失敗均以 message 回報，而非 HTTP 錯誤碼。
- **`jobClass` 找不到**：確認填入的是完整類別名稱（含套件路徑），且該類別存在於 classpath 中。

:::tip 最佳實踐
排程業務邏輯務必加入例外處理與冪等設計：即使同一任務被重複觸發或失敗重試，也不應造成資料重複或不一致。`QuartzJobFactory` 已攔截例外並記錄 log，但業務層仍應妥善處理可預期的錯誤。對於耗時任務，建議記錄開始與結束時間以利監控。
:::
