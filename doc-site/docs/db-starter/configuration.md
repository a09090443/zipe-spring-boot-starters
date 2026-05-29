---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `db-spring-boot-starter` 的所有可設定屬性。設定主要集中於 `zipe.datasource.*`，用於宣告主資料來源與多組命名資料來源。

## 資料來源頂層屬性

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.datasource.primary` | String | 無 | 預設使用的資料來源名稱 | 是 |
| `zipe.datasource.sources` | Map | 無 | 命名資料來源集合 | 是 |

## 單一資料來源屬性

`zipe.datasource.sources.<name>` 之下可設定的屬性如下：

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `url` | String | 無 | JDBC 連線字串 | 是 |
| `username` | String | 無 | 資料庫帳號 | 是 |
| `password` | String | 無 | 資料庫密碼 | 是 |
| `driver-class-name` | String | 依 url 推斷 | JDBC 驅動類別名稱 | 否 |
| `max-pool-size` | Integer | `10` | 連線池最大連線數 | 否 |
| `min-idle` | Integer | `2` | 連線池最小閒置連線數 | 否 |
| `connection-timeout` | Long | `30000` | 取得連線逾時（毫秒） | 否 |

## P6Spy 監控屬性

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.datasource.p6spy.enabled` | Boolean | `false` | 是否啟用 P6Spy SQL 監控 | 否 |
| `zipe.datasource.p6spy.log-slow-sql-millis` | Long | `1000` | 慢查詢門檻（毫秒） | 否 |

## 完整 application.yml 範例

```yaml
zipe:
  datasource:
    primary: master
    p6spy:
      enabled: true
      log-slow-sql-millis: 800
    sources:
      master:
        url: jdbc:mysql://localhost:3306/main_db?useSSL=false&serverTimezone=Asia/Taipei
        username: root
        password: ${DB_MASTER_PASSWORD}
        driver-class-name: com.mysql.cj.jdbc.Driver
        max-pool-size: 20
        min-idle: 5
        connection-timeout: 30000
      report:
        url: jdbc:postgresql://localhost:5432/report_db
        username: reader
        password: ${DB_REPORT_PASSWORD}
        driver-class-name: org.postgresql.Driver
        max-pool-size: 10
        min-idle: 2
```

:::warning ThreadLocal 清除
動態切換以 `ThreadLocal` 儲存當前資料來源。模組已在 AOP 切面的 `finally` 區塊自動清除，但若您自行操作 `DataSourceHolder`，務必在使用後呼叫清除方法，否則執行緒重用時將連到錯誤的資料庫。
:::

:::note 驅動相依
本模組不會自動帶入資料庫 JDBC 驅動，請依實際使用的資料庫於業務專案 `pom.xml` 中加入對應驅動（如 `mysql-connector-j`、`postgresql`）。
:::

:::info 多種資料庫混用
`sources` 中的各資料來源可使用不同類型的資料庫（例如 master 用 MySQL、report 用 PostgreSQL），只要提供對應的 `driver-class-name` 與驅動依賴即可。
:::
