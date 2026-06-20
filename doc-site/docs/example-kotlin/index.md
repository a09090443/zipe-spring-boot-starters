---
id: index
title: example-kotlin（Kotlin 範例）
sidebar_position: 1
description: starters_example 的 Kotlin 對應版，以 Kotlin + Gradle 1:1 重寫同一套整合範例
---

# example-kotlin（Kotlin 範例）

`example-kotlin` 是 [`starters_example`](../integration/index.md) 的 **Kotlin 對應版**，以 Kotlin + Gradle 1:1 重寫同一套整合範例，展示 zipe-spring-boot-starters 各模組在 **Kotlin 專案**中的使用方式。它與 `starters_example` 的套件結構（`com.example.*`）、類別名稱、方法簽章與對外端點**完全對齊**，差別僅在語言、建構工具與測試框架——因此兩版範例可互相對照，方便 Kotlin 使用者直接套用。

## 技術規格

| 面向 | 內容 |
|---|---|
| 語言 | Kotlin 2.2.x |
| 平台 | Spring Boot 4.0.0 / Java 17（bytecode target 17） |
| 建構工具 | Gradle（Kotlin DSL，`build.gradle.kts`） |
| 測試框架 | Kotest（`FunSpec` + `kotest-extensions-spring`） |
| Starter 座標 | `io.github.a09090443:*-spring-boot-starter:4.0.0.1`（七個 starter） |

:::note 為何另存一份 Kotlin 範例
`starters_example` 以 Java + Maven 呈現；`example-kotlin` 則保留相同功能，改以 Kotlin 慣例（`data class`、`val/var`、null 安全、不使用 Lombok）與 Gradle 建構，作為 Kotlin 專案引入這些 Starter 的參考實作。功能本身的詳細說明請參閱[整合範例教學](../integration/index.md)，本章節聚焦於 Kotlin 版的取得、啟動與語言層面的差異。
:::

## 涵蓋情境

與 `starters_example` 相同，一次整合全部七個 Starter：

| Starter | 在範例中的用途 |
|---|---|
| `base-spring-boot-starter` | 加解密（CryptoUtil）、Excel 匯入匯出、JasperReport 報表、HTTP 工具 |
| `db-spring-boot-starter` | 多資料來源動態切換（`@DS` / `DataSourceHolder`）、BaseJDBC、SQL 外化 |
| `job-spring-boot-starter` | Quartz 排程（Annotation、DB、Properties 三種模式） |
| `logon-spring-boot-starter` | Spring Security 登入認證（含 JWT cookie 混合模式） |
| `iam-spring-boot-starter` | 帳號／群組／權限身分授權，以 `@PreAuthorize` 保護端點 |
| `web-spring-boot-starter` | Thymeleaf 視圖、JSP 與統一回應格式 |
| `web-service-spring-boot-starter` | Apache CXF SOAP WebService 服務端與客戶端 |

## 專案目錄結構

```
example-kotlin/
├── build.gradle.kts                          # Gradle Kotlin DSL 建構腳本
├── settings.gradle.kts
├── gradle/wrapper/                           # Gradle 8.14 wrapper
├── src/main/kotlin/com/example/
│   ├── Application.kt                        # Spring Boot 入口（runApplication）
│   ├── config/                               # 安全設定（basic.users 覆寫、custom DbAuthProvider、JWT cookie 混合、登入日誌）
│   ├── controller/                           # RestfulController、DbExampleController（多資料源切換 API）、WebController、IamDemoController
│   ├── service/                              # ExampleService、DBExampleService（多資料源）
│   ├── job/                                  # 三種 Quartz 排程模式
│   ├── model/ + repository/                  # JPA Entity（class + var）與 Repository
│   ├── jdbc/ExampleJdbc.kt                   # 繼承 BaseJDBC
│   └── webservice/                           # CXF SOAP 介面與實作
├── src/main/resources/                       # application.yml、init SQL、iam-demo.sql、jasperreport、quartz、logback
├── src/main/webapp/WEB-INF/                  # JSP / Thymeleaf 視圖
├── src/postman/                              # Postman / SOAP 測試集合
└── src/test/kotlin/com/example/              # Kotest 測試（FunSpec + SpringExtension）
```

## 延伸閱讀

- [快速啟動](./quickstart.md) — 取得依賴、啟動專案、驗證端點
- [Kotlin 化重點](./kotlin-notes.md) — 與 `starters_example` 的語言層面差異與建構設定
- [整合範例教學](../integration/index.md) — 各 Starter 協同運作的功能說明（與本範例同樣適用）
