---
id: index
title: web-spring-boot-starter
sidebar_position: 1
description: 提供 JSP / Thymeleaf 視圖切換與統一 REST 回應格式的前端顯示 Starter
---

# web-spring-boot-starter

`web-spring-boot-starter` 處理前端顯示相關需求，支援 **JSP** 與 **Thymeleaf** 兩種視圖引擎，可透過設定切換。除了視圖渲染之外，模組還提供統一的 REST 回應格式（透過 `@ResponseResultBody` 自動包裝）、基礎 Controller、靜態資源配置、i18n 語系切換與嵌入式 Tomcat 調優，協助快速搭建 Web 應用骨架。

## 功能概述

`WebAutoConfiguration` 配置基礎 Web 環境，`ViewResolverAutoConfiguration` 依 `web.jsp.enable` / `web.thymeleaf.enable` 設定選擇 JSP 或 Thymeleaf 視圖解析器。`ResponseResultBodyAdvice` 攔截標注 `@ResponseResultBody` 的回傳值，統一包裝為 `Result<T>` 結構，使前端能以一致格式處理回應。

## 主要特性

- **雙視圖引擎**：JSP 與 Thymeleaf 可各自獨立啟用，透過 `web.jsp.enable` / `web.thymeleaf.enable` 控制。
- **統一回應格式**：`@ResponseResultBody` 自動包裝為 `Result<T>`（含 `code`、`message`、`data` 三欄位）。
- **全域例外處理**：`ResponseResultBodyAdvice` 攔截所有例外，`ResultException` 攜帶 `ResultStatus` 轉成標準錯誤回應。
- **可擴充狀態碼**：業務系統實作 `IResultStatus` 介面，自訂錯誤碼與 HTTP 狀態碼。
- **i18n 語系切換**：`LocaleChangeInterceptor` 攔截 URL 參數 `language`，搭配 `CookieLocaleResolver` 持久化語系選擇。
- **日期格式統一**：`DateFormatter` 將前端傳入的毫秒 timestamp 字串自動綁定為 `java.util.Date`。
- **靜態資源配置**：透過 `web.resource.pathPattern` / `web.resource.location` 集中管理靜態資源映射。
- **嵌入式 Tomcat 調優**：`TomcatAutoConfiguration` 關閉 JAR manifest 掃描，加速啟動速度。

## Maven 依賴引入

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>web-spring-boot-starter</artifactId>
    <version>3.5.14.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `web-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

| 類別 | 套件 | 職責 |
|---|---|---|
| `WebAutoConfiguration` | `autoconfiguration` | Web 基礎自動配置入口；注冊靜態資源 Handler、語系攔截器、DateFormatter |
| `ViewResolverAutoConfiguration` | `autoconfiguration` | 條件式注冊 JSP / Thymeleaf ViewResolver 及 LocaleResolver |
| `TomcatAutoConfiguration` | `autoconfiguration` | 客製化 TomcatServletWebServerFactory，關閉 JAR manifest 掃描 |
| `ResponseResultBody` | `annotation` | 複合注解；標記 Controller 或方法啟用統一回應包裝，同時具備 `@ResponseBody` 效果 |
| `ResponseResultBodyAdvice` | `advice` | 攔截並包裝回應為 `Result<T>`；全域攔截所有例外 |
| `Result<T>` | `dto` | 統一回應 DTO，欄位：`code`（Integer）、`message`（String）、`data`（T） |
| `IResultStatus` | `exception` | 狀態策略介面；業務可自行實作定義錯誤碼 |
| `ResultStatus` | `enums` | 內建三個預設狀態：SUCCESS(200)、BAD_REQUEST(400)、INTERNAL_SERVER_ERROR(500) |
| `ResultException` | `exception` | 業務受檢例外，攜帶 `ResultStatus`，由 Advice 攔截後轉成標準 HTTP 回應 |
| `BaseController` | `base/controller` | 抽象基礎 Controller，注入 MessageSource（i18n）與 Environment |
| `WebPropertyConfig` | `config` | 頂層屬性聚合，前綴 `web`，子組：`resource`、`jsp`、`thymeleaf` |
| `DateFormatter` | `util` | 實作 Spring `Formatter<Date>`；前端傳入毫秒 timestamp 字串雙向轉換 |

## 快速導航

- [快速開始](./quickstart.md)：建立第一個頁面與 REST API。
- [配置參考](./configuration.md)：視圖引擎與資源屬性設定。
- [使用範例](./examples.md)：統一回應與視圖渲染範例。
- [架構與開發指南](./architecture.md)：套件結構、核心類別詳解、協作流程、擴充方式。

:::tip 視圖引擎建議
新專案建議採用 Thymeleaf，其與 Spring Boot 整合度高、不需額外的 Servlet 容器設定；JSP 適用於需相容既有 JSP 頁面的舊系統遷移。
:::
