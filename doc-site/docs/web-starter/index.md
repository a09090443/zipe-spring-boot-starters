---
id: index
title: web-spring-boot-starter
sidebar_position: 1
description: 提供 JSP / Thymeleaf 視圖切換與統一 REST 回應格式的前端顯示 Starter
---

# web-spring-boot-starter

`web-spring-boot-starter` 處理前端顯示相關需求，支援 **JSP** 與 **Thymeleaf** 兩種視圖引擎，可透過設定切換。除了視圖渲染之外，模組還提供統一的 REST 回應格式（透過 `@ResponseResultBody` 自動包裝）、基礎 Controller、靜態資源配置與嵌入式 Tomcat 設定，協助快速搭建 Web 應用骨架。

## 功能概述

`WebAutoConfiguration` 配置基礎 Web 環境，`ViewResolverAutoConfiguration` 依設定選擇 JSP 或 Thymeleaf 視圖解析器。`ResponseResultBodyAdvice` 攔截標註 `@ResponseResultBody` 的回傳值，統一包裝為 `Result` 結構，使前端能以一致格式處理回應。

## 主要特性

- **雙視圖引擎**：JSP 與 Thymeleaf 可由設定切換。
- **統一回應格式**：`@ResponseResultBody` 自動包裝為 `Result`（含狀態碼、訊息、資料）。
- **基礎 Controller**：`BaseController` 提供共用方法。
- **全域例外處理**：透過 `ResultException` 與 `ResultStatus` 統一錯誤回應。
- **靜態資源配置**：`WebResourceConfig` 集中管理靜態資源映射。
- **嵌入式 Tomcat 設定**：`TomcatAutoConfiguration` 調整伺服器參數。

## Maven 依賴引入

```xml
<dependency>
    <groupId>com.zipe</groupId>
    <artifactId>web-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `web-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

| 類別 | 職責 |
|---|---|
| `WebAutoConfiguration` | Web 基礎自動配置入口 |
| `ViewResolverAutoConfiguration` | JSP / Thymeleaf 視圖解析器自動配置 |
| `TomcatAutoConfiguration` | 嵌入式 Tomcat 自動配置 |
| `ResponseResultBody` | 統一回應包裝 Annotation |
| `ResponseResultBodyAdvice` | 攔截並包裝回應的 Advice |
| `Result` | 統一回應 DTO（狀態、訊息、資料） |
| `ResultStatus` | 回應狀態列舉 |
| `ResultException` | 自訂例外 |
| `BaseController` | 基礎 Controller |
| `DateFormatter` | 日期格式化工具 |

## 快速導航

- [快速開始](./quickstart.md)：建立第一個頁面與 REST API。
- [配置參考](./configuration.md)：視圖引擎與資源屬性設定。
- [使用範例](./examples.md)：統一回應與視圖渲染範例。

:::tip 視圖引擎建議
新專案建議採用 Thymeleaf，其與 Spring Boot 整合度高、不需額外的 Servlet 容器設定；JSP 適用於需相容既有 JSP 頁面的舊系統遷移。
:::
