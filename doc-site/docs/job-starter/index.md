---
id: index
title: job-spring-boot-starter
sidebar_position: 1
description: 基於 Quartz 的排程管理 Starter，支援資料庫或記憶體儲存模式
---

# job-spring-boot-starter

`job-spring-boot-starter` 封裝了 Quartz 排程框架，提供排程的新增、修改、刪除、暫停與恢復等完整生命週期管理，並對外暴露 REST API 供前端或其他服務操作。排程儲存模式可選擇 **JDBC**（持久化至資料庫，重啟後保留）或 **記憶體**（RAMJobStore，重啟後清空），適用於不同的部署需求。

## 功能概述

業務 Job 只需繼承 `QuartzJobFactory` 並覆寫 `executeJob()` 方法，即可利用 Template Method 框架獲得標準化的 before/after/error 日誌。透過 `QuartzController` 的 REST API 或 `quartz-jobs.properties` 的靜態定義，均可將 Job 動態或批次註冊至 Scheduler。

## 主要特性

- **完整生命週期管理**：透過 `BaseJob` 提供 upsert / 暫停 / 恢復 / 刪除 / 立即執行等五種操作，`QuartzController` 繼承 `BaseJob` 直接對外暴露為 REST API。
- **雙儲存模式**：支援 JDBC 持久化與記憶體儲存，由 `spring.quartz.job-store-type` 設定切換。
- **REST API**：`QuartzController` 提供 `POST /quartz/register|delete|pause|resume|run` 五個端點。
- **Template Method 執行框架**：`QuartzJobFactory` 封裝 before/after/error 日誌，業務 Job 只需實作 `executeJob()`。
- **屬性驅動批次初始化**：`InitialJobAutoConfiguration` 讀取 `quartz-jobs.properties`，應用啟動時自動批次建立 Cron 排程。
- **Spring 整合**：配合 `SpringBeanJobFactory` 設定後，排程任務可透過 `@Autowired` 注入 Spring Bean。

## Maven 依賴引入

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>job-spring-boot-starter</artifactId>
    <version>3.5.11.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `job-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

| 類別 | 套件 | 職責 |
|---|---|---|
| `InitialJobAutoConfiguration` | `autoconfiguration` | 讀取 `quartz-jobs.properties`，啟動時批次建立排程；自動 Import `QuartzController` |
| `DataSourceAutoConfiguration` | `autoconfiguration` | JDBC 模式下建立 Quartz 專屬 HikariCP DataSource |
| `BaseJob` | `base` | 排程生命週期管理抽象類別（mergeJobProcess / deleteJobProcess / pauseJobProcess / resumeJobProcess / runJobProcess） |
| `QuartzJobFactory` | `job` | 排程執行框架（Template Method）；業務 Job 繼承此類並覆寫 `executeJob()` |
| `QuartzJobUtil` | `util` | 純工具類別，建構 `JobDetail` 與 `Trigger`，不持有 Scheduler 參照 |
| `QuartzController` | `controller` | 繼承 `BaseJob`，提供 `POST /quartz/register|delete|pause|resume|run` |
| `ScheduleEnum` | `enums` | 時間單位列舉（NOW/SECOND/MINUTE/HOUR/DAY/WEEK/MONTH/YEAR/CRON），封裝 ScheduleBuilder 建立邏輯 |
| `ScheduleJobStatusEnum` | `enums` | 操作意圖列舉（MERGE/DELETE/PAUSE/RESUME/ONCE 等） |
| `ScheduleJobVO` | `vo` | REST API 請求/回應傳輸物件（含 jobName、jobClass、cronExpression、jobDataMap 等欄位） |
| `Job` | `model` | 排程領域模型；欄位名稱與 ScheduleJobVO 不同，由 `BaseJob.convertToJob()` 映射 |
| `QuartzDataSourceProperties` | `config` | 綁定 `spring.datasource.quartz.*` 屬性 |
| `QuartzJobPropertyConfig` | `config` | 綁定 `quartz.job-map.*` 批次排程定義 |

## 快速導航

- [快速開始](./quickstart.md)：撰寫第一個排程任務並啟用。
- [配置參考](./configuration.md)：Quartz 屬性與儲存模式設定。
- [使用範例](./examples.md)：動態註冊與 REST API 操作範例。
- [架構與開發指南](./architecture.md)：套件結構、核心類別協作、擴充情境與維護陷阱。

:::tip JDBC 模式需求
若選用 JDBC 儲存模式，需事先在資料庫建立 Quartz 的標準資料表（`QRTZ_*`）。詳見配置參考頁。
:::
