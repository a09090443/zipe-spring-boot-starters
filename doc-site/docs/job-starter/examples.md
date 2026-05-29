---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁示範 `job-spring-boot-starter` 的排程撰寫、動態管理與 REST API 操作。

## 基礎使用範例

### 撰寫一個排程任務

```java
import com.zipe.quartz.base.BaseJob;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CleanupJob extends BaseJob {

    @Autowired
    private CleanupService cleanupService;

    @Override
    protected void doExecute(JobExecutionContext context) {
        int removed = cleanupService.removeExpiredData();
        System.out.println("清除過期資料 " + removed + " 筆");
    }
}
```

## 進階使用範例

### 動態管理排程生命週期

```java
import com.zipe.quartz.model.Job;
import com.zipe.quartz.util.QuartzJobUtil;
import org.springframework.stereotype.Service;

@Service
public class ScheduleManager {

    private final QuartzJobUtil quartzJobUtil;

    public ScheduleManager(QuartzJobUtil quartzJobUtil) {
        this.quartzJobUtil = quartzJobUtil;
    }

    public void add() {
        Job job = new Job();
        job.setJobName("cleanupJob");
        job.setJobGroup("maintenance");
        job.setJobClass("com.example.CleanupJob");
        job.setCronExpression("0 0 2 * * ?"); // 每天凌晨 2 點
        quartzJobUtil.addJob(job);
    }

    public void pause() {
        quartzJobUtil.pauseJob("cleanupJob", "maintenance");
    }

    public void resume() {
        quartzJobUtil.resumeJob("cleanupJob", "maintenance");
    }

    public void remove() {
        quartzJobUtil.deleteJob("cleanupJob", "maintenance");
    }
}
```

### 修改排程的 Cron 表示式

```java
public void reschedule(QuartzJobUtil quartzJobUtil) {
    Job job = new Job();
    job.setJobName("cleanupJob");
    job.setJobGroup("maintenance");
    job.setCronExpression("0 0 3 * * ?"); // 改為凌晨 3 點
    quartzJobUtil.updateJob(job);
}
```

## 常見情境

### 情境一：透過 REST API 操作排程

模組內建的 `QuartzController` 提供管理端點，可直接以 HTTP 操作：

```bash
# 查詢所有排程
curl http://localhost:8080/quartz/list

# 暫停排程
curl -X POST "http://localhost:8080/quartz/pause?jobName=cleanupJob&jobGroup=maintenance"

# 恢復排程
curl -X POST "http://localhost:8080/quartz/resume?jobName=cleanupJob&jobGroup=maintenance"
```

### 情境二：應用啟動時自動註冊預設排程

```java
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class JobInitializer {

    private final ScheduleManager scheduleManager;

    public JobInitializer(ScheduleManager scheduleManager) {
        this.scheduleManager = scheduleManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initJobs() {
        scheduleManager.add();
    }
}
```

### 情境三：排程任務存取資料庫

排程任務本身為 Spring Bean，可注入 Repository 或 Service 存取資料庫，與一般業務邏輯撰寫方式相同。

## 常見問題

- **排程沒有觸發**：檢查 `auto-startup` 是否為 `true`，以及 Cron 表示式是否正確（Quartz 為 6 至 7 欄位）。
- **重啟後排程消失**：記憶體模式不會持久化，請改用 JDBC 模式。
- **JDBC 模式啟動失敗**：通常是未建立 `QRTZ_*` 資料表，請執行對應資料庫的建表腳本。
- **Job 中無法注入 Bean**：確認 Job 類別已標註 `@Component`，由 `QuartzJobFactory` 交給 Spring 管理。

:::tip 最佳實踐
排程業務邏輯務必加入例外處理與冪等設計：即使同一任務被重複觸發或失敗重試，也不應造成資料重複或不一致。對於耗時任務，建議記錄開始與結束時間以利監控。
:::
