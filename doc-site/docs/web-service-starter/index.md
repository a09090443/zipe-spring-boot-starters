---
id: index
title: web-service-spring-boot-starter
sidebar_position: 1
description: 基於 Apache CXF 的 SOAP WebService Starter，提供服務端與客戶端整合
---

# web-service-spring-boot-starter

`web-service-spring-boot-starter` 基於 Apache CXF，提供 SOAP WebService 的服務端自動註冊與客戶端呼叫能力。服務端只需以 Annotation 標註介面，即可自動發布為 SOAP 端點；客戶端則透過 `WebServiceClientUtil` 簡化遠端呼叫。模組另內建 CDATA 攔截器與客戶端認證攔截器，處理常見的 XML 與安全需求。

## 功能概述

`CxfConfigAutoConfiguration` 向 Servlet 容器注冊 CXF Servlet 並綁定 URL 前綴；`WebServiceRegisterAutoConfiguration` 在容器啟動時依設定動態發布所有 SOAP 端點。CDATA 攔截器透明地在傳輸過程中正確包裹與解析 CDATA 區段，避免特殊字元造成 XML 解析錯誤。

## 主要特性

- **服務端自動發布**：設定 `web.service.map` 後重啟即可發布 SOAP 端點，無需額外程式碼。
- **客戶端動態呼叫**：`WebServiceClientUtil` 以動態 WSDL 解析封裝遠端呼叫，不需要編譯期 stub。
- **透明 CDATA 處理**：`CdataContentInterceptor`（入）、`ResponseCdataInterceptor`（出）自動處理 HTML 實體轉義，業務程式碼無感知。
- **欄位級 CDATA**：`CdataAdapter` 搭配 `@XmlJavaTypeAdapter` 在 JAXB 欄位層級精確控制 CDATA 包裹。
- **客戶端認證**：`ClientLoginInterceptor` 為客戶端請求插入 SOAP Header 認證資訊。
- **XML 與 SOAP 工具**：`XmlUtil`（Jackson XmlMapper 封裝）、`SoapUtil`（底層 HTTP POST）處理 XML 與 SOAP 訊息。

## Maven 依賴引入

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>web-service-spring-boot-starter</artifactId>
    <version>4.0.0.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `web-service-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

| 類別 | 套件 | 職責 |
|---|---|---|
| `CxfConfigAutoConfiguration` | `com.zipe.autoconfiguration` | 向 Servlet 容器注冊 CXF Servlet，綁定 `web.service.uri-mapping` 所設定的 URL 前綴 |
| `WebServiceRegisterAutoConfiguration` | `com.zipe.autoconfiguration` | 在容器啟動時遍歷設定，動態建立並發布所有 SOAP 端點（`InitializingBean`） |
| `WebServicePropertyConfig` | `com.zipe.config` | `@ConfigurationProperties(prefix = "web.service")` 屬性綁定入口 |
| `Service` | `com.zipe.config` | 單一端點的設定 POJO（`beanName` + `uriMapping`） |
| `WebServiceClientUtil` | `com.zipe.util` | 封裝 `JaxWsDynamicClientFactory`，動態呼叫遠端 SOAP 服務，不需編譯期 stub |
| `ClientLoginInterceptor` | `com.zipe.util` | 客戶端 SOAP Header 認證攔截器（`Phase.PREPARE_SEND`），插入自訂 `SecurityHeader` |
| `CdataContentInterceptor` | `com.zipe.interceptor` | CXF 入站攔截器（`Phase.RECEIVE`），在 JAXB 解析前還原 HTML 實體轉義 |
| `ResponseCdataInterceptor` | `com.zipe.interceptor` | CXF 出站攔截器（`Phase.PRE_STREAM`），在送出前還原 JAXB 二次轉義的 HTML 實體 |
| `CdataAdapter` | `com.zipe.adapt` | JAXB `XmlAdapter`，欄位層級的 CDATA marshal / unmarshal |
| `SoapUtil` | `com.zipe.util` | Apache HttpClient 底層 SOAP HTTP POST 工具 + SOAP 回應標籤解析工具 |
| `XmlUtil` | `com.zipe.util` | Jackson `XmlMapper` 靜態工具，Java 物件 ↔ XML 字串轉換 |

## 快速導航

- [快速開始](./quickstart.md)：發布第一個 SOAP 服務並呼叫。
- [配置參考](./configuration.md)：CXF 路徑與端點屬性設定。
- [使用範例](./examples.md)：服務端與客戶端的完整範例。
- [架構與開發指南](./architecture.md)：核心類別協作流程、擴充方式與維護注意事項。

:::tip SOAP 與 REST 的取捨
若整合對象為既有的企業系統或政府介接服務，SOAP 仍是常見選擇。新建的內部服務若無相容需求，建議優先採用 REST。
:::
