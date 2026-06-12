---
description: db-spring-boot-starter 的功能說明與 doc-site 文件導覽
paths:
  - db-spring-boot-starter/**
---

# db-spring-boot-starter

資料庫功能的 Starter，支援單一或多個資料來源的動態切換。

## 主要功能領域

多資料來源動態切換（`@DS` Annotation）、基礎 JDBC 封裝（`BaseJDBC`）、SQL 查詢條件建構（`Conditions` / `Paging`）、P6Spy SQL 監控日誌、SQL 語句外化至 XML。

## doc-site 文件導覽

工作於本模組時，依需求閱讀對應文件：

| 需求 | doc-site 文件 |
|---|---|
| 了解模組整體功能與主要類別清單 | [index.md](../../doc-site/docs/db-starter/index.md) |
| 引入 Maven 依賴與最小 DataSource 設定步驟 | [quickstart.md](../../doc-site/docs/db-starter/quickstart.md) |
| 查詢 `dynamic.*` 屬性與 `DynamicDataSourceConfig` 欄位（primary、data-source-map 等） | [configuration.md](../../doc-site/docs/db-starter/configuration.md) |
| 查詢 `@DS` 切換資料來源、`BaseJDBC` 繼承、SQL 外化至 XML、`Conditions`/`Paging` 建構查詢的用法 | [examples.md](../../doc-site/docs/db-starter/examples.md) |
| 了解 `DynamicDataSource` ThreadLocal 切換機制、AOP 切面原理、擴充指南 | [architecture.md](../../doc-site/docs/db-starter/architecture.md) |
