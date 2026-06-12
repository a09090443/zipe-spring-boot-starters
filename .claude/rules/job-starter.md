---
description: job-spring-boot-starter 的功能說明與 doc-site 文件導覽
paths:
  - job-spring-boot-starter/**
---

# job-spring-boot-starter

排程功能的 Starter，基於 Quartz 框架，支援 JDBC 資料庫或記憶體兩種 JobStore 模式。

## 主要功能領域

Quartz 排程生命週期管理（新增 / 修改 / 刪除 / 暫停 / 恢復）、`QuartzJobFactory` 可繼承自訂業務邏輯、`QuartzJobUtil` 管理工具、排程管理 REST API（`QuartzController`）、`BaseJob` 基礎類別。

## doc-site 文件導覽

工作於本模組時，依需求閱讀對應文件：

| 需求 | doc-site 文件 |
|---|---|
| 了解模組整體功能與主要類別清單 | [index.md](../../doc-site/docs/job-starter/index.md) |
| 引入 Maven 依賴與 Quartz 基本設定步驟 | [quickstart.md](../../doc-site/docs/job-starter/quickstart.md) |
| 查詢 `quartz.*`（job-map、allowed-job-classes）與 `spring.datasource.quartz.*` 屬性 | [configuration.md](../../doc-site/docs/job-starter/configuration.md) |
| 查詢繼承 `QuartzJobFactory` 撰寫業務邏輯、`QuartzJobUtil` 管理排程、REST API 操作排程的用法 | [examples.md](../../doc-site/docs/job-starter/examples.md) |
| 了解 JDBC / 記憶體 JobStore 選擇、Bean 結構、自動配置原理、擴充指南 | [architecture.md](../../doc-site/docs/job-starter/architecture.md) |
