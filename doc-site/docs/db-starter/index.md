---
id: index
title: db-spring-boot-starter
sidebar_position: 1
description: 支援多資料來源動態切換、JDBC 封裝與 SQL 條件建構的資料庫 Starter
---

# db-spring-boot-starter

`db-spring-boot-starter` 提供資料庫存取的進階能力，最核心的特性是**多資料來源動態切換**：透過 `@DS` Annotation 標註方法或類別，即可在執行期間自動切換至指定的資料庫連線，無須手動管理多組 `DataSource`。此外，模組另封裝了基礎 JDBC 操作與 SQL 查詢條件建構器，並整合 P6Spy 進行 SQL 監控。

## 功能概述

模組以 AOP 切面（`DynamicDataSourceAspect`）攔截帶有 `@DS` Annotation 的方法呼叫，在進入方法前將目標資料來源寫入 `ThreadLocal`（`DataSourceHolder`），由 `DynamicDataSource` 依此決定實際連線，方法結束後再清除 `ThreadLocal`，確保執行緒安全。

## 主要特性

- **多資料來源動態切換**：以 `@DS("dsName")` 指定方法使用的資料來源。
- **動態資料來源**：透過 `@DynamicDS` 支援執行期決定資料來源。
- **基礎 JDBC 封裝**：`BaseJDBC` 提供常用的查詢、更新方法。
- **SQL 條件建構器**：`SQL`、`Conditions`、`Paging` 協助組裝動態查詢與分頁。
- **P6Spy SQL 監控**：`P6SpyLogger` 輸出實際執行的 SQL 與耗時，便於除錯與效能調校。

## Maven 依賴引入

```xml
<dependency>
    <groupId>com.zipe</groupId>
    <artifactId>db-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `db-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

| 類別 | 職責 |
|---|---|
| `DataSourceConfigAutoConfiguration` | 資料來源自動配置入口 |
| `DataSourceAspectAutoConfiguration` | 動態切換 AOP 切面自動配置 |
| `DS` | 指定資料來源的方法／類別 Annotation |
| `DynamicDS` | 動態（執行期）資料來源 Annotation |
| `DynamicDataSourceAspect` | 攔截 `@DS` 並切換資料來源的 AOP 切面 |
| `DataSourceHolder` | 以 ThreadLocal 持有當前資料來源 |
| `DynamicDataSource` | 依 `DataSourceHolder` 路由的資料來源實作 |
| `BaseJDBC` | 基礎 JDBC 操作封裝 |
| `SQL` | 動態 SQL 建構器 |
| `Conditions` | 查詢條件組裝 |
| `Paging` | 分頁條件 |
| `P6SpyLogger` | P6Spy SQL 日誌輸出 |

## 快速導航

- [快速開始](./quickstart.md)：設定多資料來源並以 `@DS` 切換。
- [配置參考](./configuration.md)：完整資料來源屬性與 `application.yml` 範例。
- [使用範例](./examples.md)：JDBC 封裝與 SQL 建構器的實際用法。

:::tip 搭配 base-starter
本模組的部分工具會使用 `base-spring-boot-starter` 的字串／轉換工具，建議一併安裝。
:::
