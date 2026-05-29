---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `base-spring-boot-starter` 的所有可設定屬性，並提供完整的 `application.yml` 範例。屬性主要分為郵件設定、執行緒池設定與 Velocity 樣板設定三大類。

## 郵件設定屬性

郵件相關屬性分為兩部分：Spring 原生的 `spring.mail.*` 連線設定，以及本模組擴充的 `zipe.mail.*` 設定。

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `spring.mail.host` | String | 無 | SMTP 伺服器主機 | 是 |
| `spring.mail.port` | Integer | `25` | SMTP 連接埠 | 否 |
| `spring.mail.username` | String | 無 | SMTP 帳號 | 否 |
| `spring.mail.password` | String | 無 | SMTP 密碼 | 否 |
| `zipe.mail.from` | String | 無 | 預設寄件者地址 | 是 |
| `zipe.mail.default-encoding` | String | `UTF-8` | 郵件內容編碼 | 否 |

## 執行緒池設定屬性

執行緒池由 `ThreadPoolTaskExecutorConfig` 配置，用於非同步郵件發送等背景任務。

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.thread-pool.core-pool-size` | Integer | `5` | 核心執行緒數 | 否 |
| `zipe.thread-pool.max-pool-size` | Integer | `10` | 最大執行緒數 | 否 |
| `zipe.thread-pool.queue-capacity` | Integer | `100` | 任務佇列容量 | 否 |
| `zipe.thread-pool.keep-alive-seconds` | Integer | `60` | 閒置執行緒存活秒數 | 否 |
| `zipe.thread-pool.thread-name-prefix` | String | `zipe-task-` | 執行緒名稱前綴 | 否 |

## Velocity 樣板設定屬性

Velocity 樣板由 `VelocityPropertyConfig` 配置，用於郵件內容套版。

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.velocity.resource-loader-path` | String | `classpath:/templates/` | 樣板檔案載入路徑 | 否 |
| `zipe.velocity.input-encoding` | String | `UTF-8` | 樣板輸入編碼 | 否 |
| `zipe.velocity.output-encoding` | String | `UTF-8` | 樣板輸出編碼 | 否 |

## 完整 application.yml 範例

以下範例包含本模組所有可設定屬性，可作為設定起點：

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: noreply@example.com
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

zipe:
  mail:
    from: noreply@example.com
    default-encoding: UTF-8
  thread-pool:
    core-pool-size: 5
    max-pool-size: 10
    queue-capacity: 100
    keep-alive-seconds: 60
    thread-name-prefix: zipe-task-
  velocity:
    resource-loader-path: classpath:/templates/
    input-encoding: UTF-8
    output-encoding: UTF-8
```

:::warning 連接埠與加密
不同 SMTP 服務商使用的連接埠不同：`25`（未加密）、`587`（STARTTLS）、`465`（SSL）。請依服務商說明設定，並務必開啟 `starttls` 或 SSL 以保護傳輸安全。
:::

:::note 樣板路徑
`resource-loader-path` 預設指向 `classpath:/templates/`，請將 Velocity 樣板檔（`.vm`）放置於 `src/main/resources/templates/` 目錄下，否則套版會找不到檔案。
:::

:::info 環境變數注入
範例中 `${MAIL_PASSWORD}` 會由 Spring 從環境變數或外部設定來源解析，建議於正式環境採用此方式管理密碼。
:::
