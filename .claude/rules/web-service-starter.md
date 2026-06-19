---
description: web-service-spring-boot-starter 的功能說明與 doc-site 文件導覽
paths:
  - web-service-spring-boot-starter/**
---

# web-service-spring-boot-starter

WebService 功能的 Starter，基於 Apache CXF，提供 SOAP WebService 的服務端自動註冊與客戶端呼叫整合。

## 主要功能領域

CXF SOAP WebService 服務端自動註冊、`WebServiceClientUtil` 客戶端動態呼叫、`SoapUtil` SOAP 訊息工具、CDATA 內容攔截器（入 / 出）、`ClientLoginInterceptor` 客戶端認證、XML 工具。

## doc-site 文件導覽

工作於本模組時，依需求閱讀對應文件：

| 需求 | doc-site 文件 |
|---|---|
| 了解模組整體功能與主要類別清單 | [index.md](../../doc-site/docs/web-service-starter/index.md) |
| 引入 Maven 依賴、定義 `@WebService` 服務與最小 `web.service.*` 設定步驟 | [quickstart.md](../../doc-site/docs/web-service-starter/quickstart.md) |
| 查詢 `web.service.*`（uri-mapping、map）及 `Service`（bean-name、uri-mapping）子屬性 | [configuration.md](../../doc-site/docs/web-service-starter/configuration.md) |
| 查詢 `WebServiceClientUtil` 呼叫遠端服務、SOAP 攔截器掛載、CDATA 處理、`SoapUtil` 用法 | [examples.md](../../doc-site/docs/web-service-starter/examples.md) |
| 了解 CXF 自動配置機制、`WebServiceRegisterAutoConfiguration` 服務掃描註冊原理、擴充指南 | [architecture.md](../../doc-site/docs/web-service-starter/architecture.md) |
