---
id: index
title: web-service-spring-boot-starter
sidebar_position: 1
description: 基於 Apache CXF 的 SOAP WebService Starter，提供服務端與客戶端整合
---

# web-service-spring-boot-starter

`web-service-spring-boot-starter` 基於 Apache CXF，提供 SOAP WebService 的服務端自動註冊與客戶端呼叫能力。服務端只需以 Annotation 標註介面，即可自動發布為 SOAP 端點；客戶端則透過 `WebServiceClientUtil` 簡化遠端呼叫。模組另內建 CDATA 攔截器與客戶端認證攔截器，處理常見的 XML 與安全需求。

## 功能概述

`CxfConfigAutoConfiguration` 配置 CXF 框架基礎環境，`WebServiceRegisterAutoConfiguration` 掃描並自動註冊標註為服務的端點。CDATA 攔截器負責在傳輸過程中正確包裹與解析 CDATA 區段，避免特殊字元造成 XML 解析錯誤。

## 主要特性

- **服務端自動註冊**：以 Annotation 宣告即可發布 SOAP 端點。
- **客戶端工具**：`WebServiceClientUtil` 封裝遠端 WebService 呼叫。
- **CDATA 攔截器**：`CdataContentInterceptor`（入）、`ResponseCdataInterceptor`（出）處理 CDATA。
- **客戶端認證**：`ClientLoginInterceptor` 為客戶端請求附加認證資訊。
- **XML 與 SOAP 工具**：`XmlUtil`、`SoapUtil` 處理 XML 與 SOAP 訊息。

## Maven 依賴引入

```xml
<dependency>
    <groupId>com.zipe</groupId>
    <artifactId>web-service-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `web-service-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

| 類別 | 職責 |
|---|---|
| `CxfConfigAutoConfiguration` | CXF 框架自動配置入口 |
| `WebServiceRegisterAutoConfiguration` | 掃描並自動註冊 WebService 端點 |
| `Service` | 標註服務端點的 Annotation |
| `WebServiceClientUtil` | WebService 客戶端呼叫工具 |
| `ClientLoginInterceptor` | 客戶端認證攔截器 |
| `CdataContentInterceptor` | 入站 CDATA 內容攔截器 |
| `ResponseCdataInterceptor` | 出站 CDATA 回應攔截器 |
| `SoapUtil` | SOAP 訊息工具 |
| `XmlUtil` | XML 處理工具 |
| `CdataAdapter` | CDATA XML 轉換器 |

## 快速導航

- [快速開始](./quickstart.md)：發布第一個 SOAP 服務並呼叫。
- [配置參考](./configuration.md)：CXF 路徑與端點屬性設定。
- [使用範例](./examples.md)：服務端與客戶端的完整範例。

:::tip SOAP 與 REST 的取捨
若整合對象為既有的企業系統或政府介接服務，SOAP 仍是常見選擇。新建的內部服務若無相容需求，建議優先採用 REST。
:::
