---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `job-spring-boot-starter` 的可設定屬性，集中於 `zipe.quartz.*`，涵蓋儲存模式、啟動行為與 JDBC 資料來源設定。

## 排程基本屬性

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.quartz.store-type` | String | `memory` | 儲存模式：`memory` 或 `jdbc` | 否 |
| `zipe.quartz.auto-startup` | Boolean | `true` | 應用啟動時是否自動啟動排程器 | 否 |
| `zipe.quartz.startup-delay` | Integer | `0` | 排程器延遲啟動秒數 | 否 |
| `zipe.quartz.overwrite-existing-jobs` | Boolean | `false` | 是否覆寫已存在的同名排程 | 否 |
| `zipe.quartz.scheduler-name` | String | `ZipeScheduler` | 排程器名稱 | 否 |

## JDBC 模式資料來源屬性

當 `store-type` 設為 `jdbc` 時，需提供 Quartz 專用的資料來源設定：

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.quartz.datasource.url` | String | 無 | Quartz 資料庫連線字串 | 是（JDBC 模式） |
| `zipe.quartz.datasource.username` | String | 無 | 資料庫帳號 | 是（JDBC 模式） |
| `zipe.quartz.datasource.password` | String | 無 | 資料庫密碼 | 是（JDBC 模式） |
| `zipe.quartz.datasource.driver-class-name` | String | 依 url 推斷 | JDBC 驅動類別 | 否 |
| `zipe.quartz.datasource.table-prefix` | String | `QRTZ_` | Quartz 資料表前綴 | 否 |

## 完整 application.yml 範例

以下為 JDBC 模式的完整範例：

```yaml
zipe:
  quartz:
    store-type: jdbc
    auto-startup: true
    startup-delay: 5
    overwrite-existing-jobs: true
    scheduler-name: ZipeScheduler
    datasource:
      url: jdbc:mysql://localhost:3306/quartz_db?useSSL=false&serverTimezone=Asia/Taipei
      username: quartz
      password: ${QUARTZ_DB_PASSWORD}
      driver-class-name: com.mysql.cj.jdbc.Driver
      table-prefix: QRTZ_
```

記憶體模式則僅需：

```yaml
zipe:
  quartz:
    store-type: memory
    auto-startup: true
    overwrite-existing-jobs: true
```

:::warning JDBC 模式需先建表
使用 JDBC 模式前，必須先在目標資料庫執行 Quartz 官方提供的建表腳本（依資料庫類型命名，如 `tables_mysql_innodb.sql`），建立 `QRTZ_*` 系列資料表，否則排程器啟動時會失敗。
:::

:::note 記憶體模式的取捨
記憶體模式無須建表、啟動最快，但所有排程會在應用重啟後遺失，且無法在多節點間共享，僅適合單機或開發測試環境。正式環境的持久化與叢集需求請使用 JDBC 模式。
:::

:::info 叢集部署
JDBC 模式搭配 Quartz 的叢集設定，可在多節點環境中避免同一排程被重複觸發。如有此需求，請於資料來源外另行設定 Quartz 的 `org.quartz.jobStore.isClustered` 等屬性。
:::
