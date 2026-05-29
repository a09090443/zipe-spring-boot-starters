---
id: quickstart
title: 快速開始
sidebar_position: 2
---

# 快速開始

本頁帶您撰寫一個簡單的排程任務，並透過 `QuartzJobUtil` 動態啟用。

## 前置需求

- JDK 17 以上、Spring Boot 3.5.x。
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
    <groupId>com.zipe</groupId>
    <artifactId>job-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Step 3：設定 application.yml

以記憶體模式為例：

```yaml
zipe:
  quartz:
    store-type: memory
    auto-startup: true
    overwrite-existing-jobs: true
```

## Step 4：程式碼範例

繼承 `BaseJob` 撰寫業務邏輯：

```java
import com.zipe.quartz.base.BaseJob;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class ReportJob extends BaseJob {

    @Override
    protected void doExecute(JobExecutionContext context) {
        System.out.println("產生報表中... " + java.time.LocalDateTime.now());
        // 撰寫實際業務邏輯
    }
}
```

以 `QuartzJobUtil` 動態註冊排程（每分鐘執行一次）：

```java
import com.zipe.quartz.model.Job;
import com.zipe.quartz.util.QuartzJobUtil;
import org.springframework.stereotype.Service;

@Service
public class JobBootstrap {

    private final QuartzJobUtil quartzJobUtil;

    public JobBootstrap(QuartzJobUtil quartzJobUtil) {
        this.quartzJobUtil = quartzJobUtil;
    }

    public void registerReportJob() {
        Job job = new Job();
        job.setJobName("reportJob");
        job.setJobGroup("report");
        job.setJobClass("com.example.ReportJob");
        job.setCronExpression("0 * * * * ?");
        quartzJobUtil.addJob(job);
    }
}
```

## Step 5：執行驗證

啟動應用程式並觸發註冊，觀察主控台是否每分鐘輸出訊息：

```bash
./mvnw spring-boot:run
```

也可透過 REST API 檢視排程清單：

```bash
curl http://localhost:8080/quartz/list
```

:::tip Cron 表示式
`0 * * * * ?` 代表「每分鐘的第 0 秒」執行。Quartz 的 Cron 為 6 至 7 欄位（秒 分 時 日 月 週 [年]），與 Linux crontab 的 5 欄位不同，請特別留意。
:::

:::warning Job class 路徑
`setJobClass` 必須填入完整的類別名稱（含套件路徑），且該類別需為 Spring 管理的 Bean，否則註冊時將找不到對應的 Job。
:::
