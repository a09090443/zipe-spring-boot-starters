---
id: index
title: 整合範例教學
sidebar_position: 1
description: 了解如何組合不同 Starter 建構實際應用系統
---

# 整合範例教學

本章節以專案內附的 `starters_example` 整合範例專案為主軸，說明如何**同時引入多個 Starter 並讓它們協同運作**，建構出貼近真實業務需求的應用系統。前面各模組文件著重於單一 Starter 的功能與設定，本章節則聚焦於「組合」的層面：不同 Starter 之間如何分工、設定檔之間如何串接、以及啟動時各模組的自動配置順序。

## starters_example 專案簡介

`starters_example` 是一個 Spring Boot 應用程式，它一次引入了 `base`、`web`、`web-service`、`job`、`db`、`logon` 六個自製 Starter，並透過單一 `application.yml` 與數個 `*.properties` 檔案完成所有設定。專案示範了以下能力：

- 以 JSP 與 Thymeleaf 同時提供前端頁面（依視圖名稱前綴路由）
- 以 RESTful API 與 WebFlux SSE 串流提供資料介面
- 以 Spring Security 提供登入認證，並透過策略模式注入自訂登入日誌
- 以動態多資料來源連接多個資料庫，支援 `@DS` 與程式化兩種切換方式
- 以 Quartz 提供三種不同模式的排程任務
- 以 Apache CXF 發布 SOAP WebService 端點

## 專案使用的 Starters 清單與用途

| Starter | 用途 | 對應設定區塊 |
|---|---|---|
| `base-spring-boot-starter` | 加解密、日期/字串工具、Excel、JasperReport 等基礎設施 | （供其他 Starter 依賴） |
| `db-spring-boot-starter` | 多資料來源動態切換、JDBC 封裝 | `spring.jpa.*`、`data-source.properties` |
| `job-spring-boot-starter` | Quartz 排程管理 | `spring.quartz.*`、`quartz-jobs.properties` |
| `logon-spring-boot-starter` | Spring Security 登入認證 | `security.*` |
| `web-service-spring-boot-starter` | Apache CXF SOAP WebService | `web.service.*` |
| `web-spring-boot-starter` | JSP / Thymeleaf 前端視圖 | `web.*` |

## 五種組合情境快速導覽

為了讓你能依需求挑選合適的 Starter 組合，本章節整理出五種典型情境，由淺入深：

1. [情境一：Web 應用含認證](./scenario-web-auth.md) — `base + web + logon`，適合需要登入保護的管理後台。
2. [情境二：多資料來源應用](./scenario-db.md) — `base + db`，適合需要連接多個資料庫的系統。
3. [情境三：排程任務應用](./scenario-job.md) — `base + job`（可選 `db`），適合需要定時執行任務的系統。
4. [情境四：SOAP WebService](./scenario-webservice.md) — `base + web-service`，適合需要對接 SOAP 協議的企業系統。
5. [情境五：全功能整合](./scenario-full.md) — 整合所有 Starter，完整呈現 `starters_example` 的實作。

## 專案目錄結構說明

`starters_example` 的主要目錄與類別功能如下：

```
starters_example/
├── src/main/java/com/example/
│   ├── Application.java                    # Spring Boot 入口
│   ├── config/
│   │   └── LogonLogRecord.java             # 自訂登入日誌（實作 CustomLogonLogRecord）
│   ├── controller/
│   │   ├── RestfulController.java          # REST API（/rest/sayHello、/rest/flux）
│   │   └── WebController.java              # 頁面路由（/jsp、/thymeleaf、/demo、/）
│   ├── service/
│   │   ├── ExampleService.java             # 範例服務介面
│   │   ├── ExampleServiceImpl.java         # sayHello 實作
│   │   ├── DBExampleService.java           # 多資料來源服務介面
│   │   └── DBExampleServiceImpl.java       # @DS / DataSourceHolder 切換示範
│   ├── job/
│   │   ├── ExampleAnnotationJob.java       # @Scheduled 模式
│   │   ├── ExampleDbJob.java               # 資料庫模式
│   │   └── ExampleXmlJob.java              # properties 設定模式
│   ├── model/
│   │   ├── UserMain.java                   # JPA Entity（user_main 表）
│   │   └── UserDetail.java                 # JPA Entity（user_detail 表）
│   ├── repository/
│   │   ├── UserMainRepository.java
│   │   └── UserDetailRepository.java
│   ├── jdbc/ExampleJdbc.java               # 繼承 BaseJDBC 的 JDBC 範例
│   └── webservice/
│       ├── ExampleWebService.java          # SOAP 介面（SEI）
│       └── impl/ExampleWebServiceImpl.java # SOAP 實作（@Component）
├── src/main/resources/
│   ├── application.yml                     # 主設定
│   ├── resources.properties                # Logback 外部化參數
│   ├── data-source.properties              # 多資料來源定義
│   ├── quartz-jobs.properties              # Quartz 靜態排程定義
│   ├── quartz-datasource.properties        # Quartz JDBC 模式資料來源（備用）
│   ├── init/schema.sql                     # 建表（user_main / user_detail，application.yml 實際引用）
│   ├── init/data.sql                       # 初始資料（application.yml 實際引用）
│   ├── init/h2/schema.sql                  # H2 建表（TBL_EMPLOYEES，備用）
│   ├── init/h2/data.sql                    # H2 初始資料（備用）
│   ├── jasperreport/*.jrxml                # JasperReport 報表模板
│   └── logback-spring.xml                  # Logback 日誌設定
├── src/main/webapp/WEB-INF/                # JSP / Thymeleaf 視圖（hello.jsp、hello.html、demo.html、index.html）
└── src/postman/
    └── Example.postman_collection.json     # Postman / SOAP 測試集合
```

## 如何取得並執行範例專案

範例專案隨主專案一起發布，取得與執行流程如下：

### 步驟一：取得專案原始碼

```bash
git clone https://github.com/a09090443/zipe-spring-boot-starters.git
cd zipe-spring-boot-starters
```

### 步驟二：取得 Starter 依賴

範例引用的 Starter（`io.github.a09090443:*:3.5.11.0`）已發布於 **Maven Central**，建構時會自動下載，一般情況**無須額外步驟**。

若你想以本地原始碼建構最新版的 Starter，可於專案根目錄執行 reactor 建構，將各 Starter 安裝至本地 Maven Repository：

```bash
mvn clean install
```

### 步驟三：啟動範例專案

```bash
cd starters_example
./mvnw spring-boot:run
```

啟動成功後，可透過以下端點驗證各功能：

| 功能 | 端點 |
|---|---|
| Thymeleaf 頁面 | `http://localhost:8080/example/thymeleaf` |
| JSP 頁面 | `http://localhost:8080/example/jsp` |
| REST API | `http://localhost:8080/example/rest/sayHello?name=John` |
| SOAP WSDL | `http://localhost:8080/example/webservice/example?wsdl` |
| H2 Console | `http://localhost:8080/example/h2-console` |

:::tip 內建 H2 與 SQL 初始化說明
範例專案內建 **H2 記憶體資料庫**並開啟 H2 Console（`spring.h2.console.enabled: true`）。需注意 `application.yml` 的 `spring.sql.init.mode` 設為 **`never`**，因此 `init/schema.sql`（`user_main` / `user_detail`）與 `init/data.sql` **不會在啟動時自動執行**，需要時請自行於 H2 Console 手動載入。`init/h2/` 下另附 `TBL_EMPLOYEES` 的建表/資料腳本作為備用範例。若要完整驗證多資料來源（`example1` / `example2`），才需要準備對應的 MySQL 環境。
:::

:::note 範例專案的版本獨立於主專案
`starters_example` 未納入主專案的 Maven reactor，但已同步至目前版本：Spring Boot `3.5.11`、`zipe.spring.starter.version` 為 `3.5.11.0`，Starter 座標為 `io.github.a09090443`（發布於 Maven Central）。實際引入時請以你採用的發布版本為準。
:::

:::info 其他範例專案
除了 `starters_example`，專案另提供 `example`（工具範例）、`example-keycloak`（Keycloak 範例）與 `example-kotlin`（Kotlin 範例），可依需求參考。
:::
