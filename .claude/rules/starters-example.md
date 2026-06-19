---
description: starters_example 整合測試範例專案的功能說明與 doc-site 文件導覽
paths:
  - starters_example/**
---

# starters_example（整合測試範例）

整合所有 Starter 的測試範例專案，以 H2 記憶體資料庫快速啟動，示範各 Starter 的使用方式與相互配合。

## 涵蓋情境

Web 頁面 + REST API（web-starter）、Spring Security 表單登入（logon-starter）、帳號／群組／權限身分授權（iam-starter，與 db-starter 共用 EntityManagerFactory）、多資料來源切換（db-starter）、Quartz 排程（job-starter）、CXF WebService（web-service-starter）、加解密 / 郵件 / HTTP 工具（base-starter）。

## doc-site 文件導覽

工作於本範例專案時，依需求閱讀對應文件：

| 需求 | doc-site 文件 |
|---|---|
| 了解範例專案整體架構與啟動方式 | [index.md](../../doc-site/docs/integration/index.md) |
| 查詢 Web 視圖與登入認證的整合設定範例 | [scenario-web-auth.md](../../doc-site/docs/integration/scenario-web-auth.md) |
| 查詢多資料來源、BaseJDBC、SQL 外化的整合設定範例 | [scenario-db.md](../../doc-site/docs/integration/scenario-db.md) |
| 查詢 Quartz 排程（Annotation / DB / Properties 三種模式）的整合設定範例 | [scenario-job.md](../../doc-site/docs/integration/scenario-job.md) |
| 查詢 CXF WebService 服務端與客戶端的整合設定範例 | [scenario-webservice.md](../../doc-site/docs/integration/scenario-webservice.md) |
| 查詢 iam 帳號／群組／權限與 db-starter 共用 EntityManagerFactory 的整合設定範例 | [scenario-iam.md](../../doc-site/docs/integration/scenario-iam.md) |
| 查詢所有 Starter 同時啟用的完整整合情境 | [scenario-full.md](../../doc-site/docs/integration/scenario-full.md) |
