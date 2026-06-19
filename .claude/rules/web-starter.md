---
description: web-spring-boot-starter 的功能說明與 doc-site 文件導覽
paths:
  - web-spring-boot-starter/**
---

# web-spring-boot-starter

前端顯示功能的 Starter，提供 JSP 與 Thymeleaf 兩種視圖引擎，可透過設定切換，並內建統一 REST 回應格式機制。

## 主要功能領域

JSP / Thymeleaf 視圖引擎切換、靜態資源路徑配置、`@ResponseResultBody` 統一回應格式、`BaseController` 基礎控制器、`Result` DTO、Tomcat 嵌入式伺服器配置。

## doc-site 文件導覽

工作於本模組時，依需求閱讀對應文件：

| 需求 | doc-site 文件 |
|---|---|
| 了解模組整體功能與主要類別清單 | [index.md](../../doc-site/docs/web-starter/index.md) |
| 引入 Maven 依賴與最小視圖設定步驟 | [quickstart.md](../../doc-site/docs/web-starter/quickstart.md) |
| 查詢 `web.resource.*`（path-pattern、location）、`web.jsp.*`（enable、view-names、suffix）、`web.thymeleaf.*`（enable、view-names、suffix、template-mode）屬性及視圖解析公式 | [configuration.md](../../doc-site/docs/web-starter/configuration.md) |
| 查詢 `@ResponseResultBody` 統一回應格式、`BaseController` 繼承、`Result` DTO 用法的程式碼範例 | [examples.md](../../doc-site/docs/web-starter/examples.md) |
| 了解 `ViewResolverAutoConfiguration` 原理、JSP 與 Thymeleaf 共存機制、`@ResponseResultBody` 攔截流程、擴充指南 | [architecture.md](../../doc-site/docs/web-starter/architecture.md) |
