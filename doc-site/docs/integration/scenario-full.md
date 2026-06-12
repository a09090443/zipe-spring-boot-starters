---
id: scenario-full
title: 情境五：全功能整合
sidebar_position: 6
description: 整合所有 Starter 建構完整的企業應用系統
---

# 情境五：全功能整合

## 情境說明

本情境是前述四種情境的集大成，完整呈現 `starters_example` 的實作：**同時引入六個 Starter**，建構一個兼具前端頁面、REST API、SOAP WebService、登入認證、多資料來源與排程任務的企業級應用系統。透過這個情境，你可以理解各 Starter 之間如何分工、設定檔如何串接，以及 Spring Boot 啟動時各模組的自動配置順序。

## 完整的 pom.xml 設定

```xml
<properties>
    <zipe.spring.starter.version>3.5.14.0</zipe.spring.starter.version>
</properties>

<dependencies>
    <!-- 六個自製 Starter -->
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>base-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>web-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>web-service-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>job-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>db-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>logon-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>

    <!-- Spring 官方與輔助依賴 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-jasper</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- 前端靜態資源 -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap</artifactId>
        <version>5.3.8</version>
    </dependency>
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>jquery</artifactId>
        <version>3.7.1</version>
    </dependency>
</dependencies>
```

## 完整的 application.yml 設定

```yaml
server:
  port: 8080
  servlet:
    context-path: /example

spring:
  config:
    import: classpath:resources.properties   # Logback 外部化參數
  main:
    allow-bean-definition-overriding: true
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    base:
      packages: com.example
  h2:
    console:
      enabled: true
  sql:
    init:
      mode: never
      schema-locations: classpath:init/schema.sql
      data-locations: classpath:init/data.sql
  quartz:
    enable: true
    job-store-type: memory

web:
  resource:
    pathPattern: /static/**
    location: /WEB-INF/static/
  jsp:
    enable: true
    viewNames: jsp/*
    stuff: .jsp
  thymeleaf:
    enable: true
    viewNames: /*, html/*, thymeleaf/*
    stuff: .html
    templateMode: HTML
  service:
    uri-mapping: /webservice/*
    map:
      example:
        beanName: exampleWebServiceImpl
        uri-mapping: /example

security:
  enable: true
  verification-type: basic
  custom-bean-name: test
  record-log-enable: true
  custom-record-log-bean: logonLogRecord
  allow-uris: /resources/**,/static/**,/webservice/**
  login-success-uri: /jsp
  login-failure-uri: /login
  ldap:
    ip: 127.0.0.1
    domain: ldap.zipe.com
    port: 389
    dn: DC=zipe,DC=local
```

### 各 properties 設定

`data-source.properties`（多資料來源，詳見[情境二](./scenario-db.md)）、`quartz-jobs.properties`（靜態排程，詳見[情境三](./scenario-job.md)）、`quartz-datasource.properties`（Quartz JDBC 模式備用）、`resources.properties`（Logback log 路徑/等級/檔案大小等參數，供 `logback-spring.xml` 透過 `<springProperty>` 讀取）。

## 系統架構圖

```
                          ┌─────────────────────────────────────┐
        瀏覽器 / Client    │       Spring Boot Application        │
            │             │       (context-path: /example)       │
            │             └─────────────────────────────────────┘
            │
    ┌───────┼──────────────────────────────────────────────────────────┐
    │       ▼                                                            │
    │  ┌─────────────────────────────────────────────────────────────┐ │
    │  │   Spring Security 過濾鏈 (logon-starter)                      │ │
    │  │   verification-type: basic (admin/admin)                     │ │
    │  │   白名單: /static/**  /resources/**  /webservice/**          │ │
    │  │        │ 登入事件 → LogonLogRecord (CustomLogonLogRecord)     │ │
    │  └────────┼─────────────────────────────────────────────────────┘ │
    │           ▼                                                        │
    │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐ │
    │  │ WebController│  │RestfulController│ ExampleWebServiceImpl     │ │
    │  │ (web-starter)│  │   (base/web)   │ (web-service-starter, CXF)│ │
    │  │ JSP/Thymeleaf│  │ /rest/sayHello │  /webservice/example?wsdl │ │
    │  │ /jsp /thyme..│  │ /rest/flux SSE │                           │ │
    │  └──────┬───────┘  └──────┬─────────┘  └──────────────────────────┘ │
    │         │                 ▼                                        │
    │         │          ┌──────────────┐                               │
    │         │          │ExampleService│                               │
    │         │          └──────────────┘                               │
    │         ▼                                                          │
    │  ┌─────────────────────────────────────────────────────────────┐ │
    │  │   多資料來源 (db-starter)                                      │ │
    │  │   DynamicDataSource (AbstractRoutingDataSource)              │ │
    │  │   @DS / DataSourceHolder(ThreadLocal) 切換                    │ │
    │  │   example1 (primary) ◄──► example2   + H2 (TBL_EMPLOYEES)     │ │
    │  └─────────────────────────────────────────────────────────────┘ │
    │                                                                    │
    │  ┌─────────────────────────────────────────────────────────────┐ │
    │  │   Quartz 排程 (job-starter)                                   │ │
    │  │   job-store-type: memory (RAMJobStore)                       │ │
    │  │   ExampleXmlJob (0/15 * * * * ? *)  + QuartzController /quartz│ │
    │  └─────────────────────────────────────────────────────────────┘ │
    │                                                                    │
    │  ┌─────────────────────────────────────────────────────────────┐ │
    │  │   base-starter（基礎設施：加解密 / 工具 / ResourceBundle）     │ │
    │  └─────────────────────────────────────────────────────────────┘ │
    └────────────────────────────────────────────────────────────────────┘
```

## 各模組之間的依賴與互動

各設定檔的協作關係如下：

```
application.yml
  ├── spring.config.import → resources.properties → logback-spring.xml
  ├── spring.quartz.*       → job-starter ← quartz-jobs.properties / quartz-datasource.properties
  ├── spring.jpa/h2/sql.*   → db-starter  ← data-source.properties / init/*.sql
  ├── web.*                 → web-starter（JSP+Thymeleaf）+ web-service-starter（CXF）
  └── security.*            → logon-starter ← LogonLogRecord.java
```

`base-starter` 為所有模組的共同底層；`logon-starter` 因依賴 Web 與 DB，於啟動流程中最後生效。

## Spring Boot 啟動時的自動配置順序

各 Starter 透過 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登記自己的 `@AutoConfiguration` 類別，啟動時依下列順序生效：

1. **base-starter**：最先載入，提供加解密工具（`AesUtil`）、通用工具與 `ResourceBundleConfig` 等基礎設施。
2. **db-starter**：讀取 `data-source.properties`，建立 `DynamicDataSource`，初始化 JPA EntityManagerFactory，啟動 H2 Console。
3. **web-starter**：依 `web.jsp.enable` / `web.thymeleaf.enable` 建立 `InternalResourceViewResolver` 與 `SpringTemplateEngine`，以 `viewNames` 路由分發。
4. **web-service-starter**：建立 `CXFServlet` 掛載至 `/webservice/*`，發布 `exampleWebServiceImpl` 為 SOAP 端點。
5. **job-starter**：讀取 `quartz-jobs.properties`，建立 `JobDetail` 與 `CronTrigger`（每 15 秒執行 `ExampleXmlJob`），使用 `RAMJobStore`。
6. **logon-starter**：建立 Spring Security 過濾鏈，註冊登入事件監聽器，透過 `custom-record-log-bean` 呼叫 `LogonLogRecord`。

## 完整的啟動與測試步驟

### 啟動

```bash
cd starters_example
./mvnw spring-boot:run
```

### 驗證各功能端點

| 功能 | 端點 | 預期結果 |
|---|---|---|
| Thymeleaf 頁面 | `http://localhost:8080/example/thymeleaf` | HTTP 200 |
| JSP 頁面 | `http://localhost:8080/example/jsp` | HTTP 200 |
| REST API | `http://localhost:8080/example/rest/sayHello?name=John` | `Hello,  John!` |
| SSE 串流 | `http://localhost:8080/example/rest/flux` | 每秒一個 `<letter:x>` 事件 |
| SOAP WSDL | `http://localhost:8080/example/webservice/example?wsdl` | WSDL 定義 |
| 排程（XmlJob） | 觀察日誌 | 每 15 秒一筆 `ExampleXmlJob` log |
| 排程（AnnotationJob） | 觀察日誌 | 每 20 秒一筆 `ExampleAnnotationJob` log |

登入頁預設帳密為 `admin/admin`，登入成功跳轉 `/jsp`，並在日誌看到 `測試登入紀錄:admin`。

### 整合測試的執行方式

```bash
cd starters_example
./mvnw test
```

測試策略分為三層：

- **整合測試層**（繼承 `TestBase`，啟動完整 Context）：
  - `RestControllerTest`、`WebControllerTest`：以 `@AutoConfigureMockMvc` 模擬 HTTP 請求。
  - `DBExampleServiceTest`：透過 Service 方法間接驗證 `DataSourceHolder` 切換（`getUserMainByName()` 內部切至 `example2`）。
  - `DynamicDataSourceSwitchTest`：直接操作 `DataSourceHolder` 驗證同類型（MySQL `example1` ↔ `example2`）切換是否真正生效。
  - `CrossDbSwitchTest`：驗證跨資料庫類型（MySQL ↔ PostgreSQL）切換，詳見[情境二](./scenario-db.md)。
- **單元測試層**（純 JUnit 5，不啟動 Context）：`CryptoUtilTest`（AES/3DES/Base64）、`TestExportBean`/`TestExportMap`/`TestImportExcel`、`JasperreportTest`。
- **手動測試層**（需應用已啟動）：`ExampleWebServiceTest`（Java 版 WebService 客戶端，含三種呼叫方式：JaxWsProxyFactoryBean、WebServiceClientUtil、JaxWsDynamicClientFactory）；`Example.postman_collection.json` 收錄相同端點的 SOAP 請求。

:::warning 整合測試需注意的外部依賴
- `DBExampleServiceTest`、`DynamicDataSourceSwitchTest`、`CrossDbSwitchTest` 均預期連線真實 MySQL（`example1` / `example2`），`CrossDbSwitchTest` 額外需要 PostgreSQL；無對應環境時會失敗。H2 在此扮演開發輔助角色（`sql.init.mode: never` 代表 H2 腳本未自動執行）。
- Excel 與 JasperReport 測試依賴本機 `D:/tmp/` 目錄的實際檔案，缺檔即失敗，屬本機手動測試性質。
- `ExampleWebServiceTest` 與 Postman 測試均需應用程式已啟動才能執行。
:::

:::info 從全功能拆解到單一情境
若你的系統只需要部分能力，不必照搬全部六個 Starter。可參考[情境一](./scenario-web-auth.md)至[情境四](./scenario-webservice.md)，挑選最小組合即可，各 Starter 之間並無強制耦合。
:::
