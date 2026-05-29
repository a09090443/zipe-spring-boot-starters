---
id: index
title: job-spring-boot-starter
sidebar_position: 1
description: 基於 Quartz 的排程管理 Starter，支援資料庫或記憶體儲存模式
---

# job-spring-boot-starter

`job-spring-boot-starter` 封裝了 Quartz 排程框架，提供排程的新增、修改、刪除、暫停與恢復等完整生命週期管理，並對外暴露 REST API 供前端或其他服務操作。排程儲存模式可選擇 **JDBC**（持久化至資料庫，重啟後保留）或 **記憶體**（RAMJobStore，重啟後清空），適用於不同的部署需求。

## 功能概述

開發者只需繼承 `BaseJob` 並實作業務邏輯，即可由 `QuartzJobUtil` 動態註冊為排程任務。`QuartzJobFactory` 負責將 Job 實例交由 Spring 容器管理，使排程任務可注入其他 Spring Bean。

## 主要特性

- **完整生命週期管理**：透過 `QuartzJobUtil` 進行新增、暫停、恢復、刪除排程。
- **雙儲存模式**：支援 JDBC 持久化與記憶體儲存，由設定切換。
- **REST API**：`QuartzController` 提供排程管理的 HTTP 端點。
- **可繼承基礎類別**：繼承 `BaseJob` 即可快速撰寫排程業務邏輯。
- **Spring 整合**：排程任務可注入 Spring Bean，與業務服務無縫協作。

## Maven 依賴引入

```xml
<dependency>
    <groupId>com.zipe</groupId>
    <artifactId>job-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `job-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

| 類別 | 職責 |
|---|---|
| `InitialJobAutoConfiguration` | 初始化排程自動配置入口 |
| `DataSourceAutoConfiguration` | Quartz 資料來源自動配置 |
| `BaseJob` | 排程任務基礎類別，供繼承實作業務邏輯 |
| `QuartzJobUtil` | 排程生命週期管理工具 |
| `QuartzJobFactory` | 將 Job 交由 Spring 管理的工廠 |
| `QuartzController` | 排程管理 REST API |
| `Job` | 排程資料模型 |
| `ScheduleJobVO` | 排程 View Object |
| `ScheduleEnum` | 排程類型列舉 |
| `ScheduleJobStatusEnum` | 排程狀態列舉 |

## 快速導航

- [快速開始](./quickstart.md)：撰寫第一個排程任務並啟用。
- [配置參考](./configuration.md)：Quartz 屬性與儲存模式設定。
- [使用範例](./examples.md)：動態註冊與 REST API 操作範例。

:::tip JDBC 模式需求
若選用 JDBC 儲存模式，需事先在資料庫建立 Quartz 的標準資料表（`QRTZ_*`）。詳見配置參考頁。
:::
