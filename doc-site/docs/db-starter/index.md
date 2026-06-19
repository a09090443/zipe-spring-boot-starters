---
id: index
title: db-spring-boot-starter
sidebar_position: 1
description: 支援多資料來源動態切換、JDBC 封裝與 SQL 條件建構的資料庫 Starter
---

# db-spring-boot-starter

`db-spring-boot-starter` 提供資料庫存取的進階能力，最核心的特性是**多資料來源動態切換**：透過 `@DS` Annotation 標註方法或類別，即可在執行期間自動切換至指定的資料庫連線，無須手動管理多組 `DataSource`。此外，模組另封裝了基礎 JDBC 操作與 SQL 查詢條件建構器，並整合 P6Spy 進行 SQL 監控。

## 功能概述

模組以 AOP 切面（`DynamicDataSourceAspect`）攔截帶有 `@DS` Annotation 的方法呼叫，在進入方法前將目標資料來源寫入 `ThreadLocal`（`DataSourceHolder`），由 `DynamicDataSource` 依此決定實際連線。使用 `@DS` 標注的方法，切面完成後 ThreadLocal 仍保留最後設定的值；若需確保執行緒安全，手動使用 `DataSourceHolder` 時需自行呼叫 `clearDataSourceName()`。

## 主要特性

- **多資料來源動態切換**：以 `@DS("dsName")` 指定方法或類別使用的資料來源。
- **SQL 外部化**：SQL 語句存放於 `.sql` 檔案，與 Java 程式碼分離，支援部署後熱換。
- **基礎 JDBC 封裝**：`BaseJDBC` 提供完整的查詢、更新方法，支援具名參數綁定。
- **SQL 條件建構器**：`Conditions`、`Paging` 協助組裝動態查詢與伺服器端分頁。
- **P6Spy SQL 監控**：`P6SpyLogger` 輸出實際執行的 SQL 與耗時，便於除錯與效能調校。
- **HikariCP 連線池**：每個資料來源獨立建立 HikariDataSource，自動配置最佳化參數。

## Maven 依賴引入

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>db-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `db-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。

本 Starter 已內建以下 JDBC 驅動（compile scope）：SQL Server（`mssql-jdbc`）、MySQL（`mysql-connector-j`）、MariaDB（`mariadb-java-client`）、AS400（`jt400`）。若不需要某驅動，可在業務專案以 `<exclusion>` 排除。
:::

## 主要類別

| 類別 | 套件 | 職責 |
|---|---|---|
| `DataSourceConfigAutoConfiguration` | `autoconfiguration` | 資料來源自動配置入口，建立所有 HikariDataSource / JPA / JDBC Beans |
| `DataSourceAspectAutoConfiguration` | `autoconfiguration` | 向 Spring 容器登錄 AOP 切面 Bean |
| `DynamicJpaRepositoriesRegistrar` | `autoconfiguration` | 依 `dynamic.base-packages` 設定程式化掃描並註冊 Spring Data JPA Repository |
| `DS` | `base.annotation` | 指定資料來源的方法／類別 Annotation，`value()` 對應 `dynamic.data-source-map` 中的 key |
| `DynamicDS` | `base.annotation` | `DS` 的執行期實作，供反射替換 Annotation 使用 |
| `AnnotationHelper` | `base.annotation` | 利用反射在執行期修改類別上的 Annotation 值 |
| `DynamicDataSourceAspect` | `base.aspect` | 攔截 `@DS` 並將資料來源 key 寫入 ThreadLocal 的 AOP 切面（`@Order(-1)`） |
| `DataSourceHolder` | `base.database` | 以 `ThreadLocal<String>` 持有當前資料來源 key |
| `DynamicDataSource` | `base.database` | 繼承 `AbstractRoutingDataSource`，依 `DataSourceHolder` 路由至正確的 DataSource |
| `BaseDataSourceConfig` | `base.database` | 提供 HikariCP 基礎設定的抽象父類 |
| `DataSourcePropertyConfig` | `base.config` | 以 `@ConfigurationProperties(prefix="dynamic")` 綁定所有資料來源屬性 |
| `DynamicDataSourceConfig` | `base.model` | 單一資料來源連線設定的 POJO（url / username / pa55word / driverClassName） |
| `P6SpyLogger` | `base.config` | P6Spy 自訂 SQL 日誌格式 |
| `BaseJDBC` | `jdbc` | 基礎 JDBC 操作封裝，子類別繼承後即可使用完整 CRUD 能力 |
| `ResourceEnum` | `enums` | 描述 SQL 檔案路徑（`/sql` 目錄 + `.sql` 副檔名）的 Enum |
| `Conditions` | `jdbc.criteria` | 鏈式 WHERE 條件建構器，最終以 `done()` 替換 SQL 中的 `${CONDITIONS}` |
| `Paging` | `jdbc.criteria` | 伺服器端分頁資料類別，從 `/sql/PAGING.sql` 讀取分頁模板 |
| `SQL` | `jdbc.criteria` | SQL 運算子常數 Enum（AND、OR、LIKE、IN、GT 等） |
| `SqlQuery<T>` | `common.model` | 查詢參數聚合 DTO，供外部業務層組裝後傳遞使用 |

## 快速導航

- [快速開始](./quickstart.md)：設定多資料來源並以 `@DS` 切換。
- [配置參考](./configuration.md)：完整資料來源屬性與 `data-source.properties` 範例。
- [使用範例](./examples.md)：JDBC 封裝與 SQL 建構器的實際用法。
- [架構與開發指南](./architecture.md)：內部設計、協作流程與擴充指南。

:::tip 搭配 base-starter
本模組依賴 `base-spring-boot-starter` 的字串、檔案與加解密工具，建議一併安裝。
:::
