---
description: web-spring-boot-starter 的目錄結構與功能說明
paths:
  - web-spring-boot-starter/**
---

# web-spring-boot-starter

前端顯示功能的 Starter，提供 JSP 與 Thymeleaf 兩種視圖引擎，可透過設定切換。

## 目錄結構

```
web-spring-boot-starter/
└── src/main/
    ├── java/com/zipe/
    │   ├── advice/
    │   │   └── ResponseResultBodyAdvice.java        # 統一回應包裝 Advice
    │   ├── annotation/
    │   │   └── ResponseResultBody.java              # 統一回應 Annotation
    │   ├── autoconfiguration/
    │   │   ├── TomcatAutoConfiguration.java         # Tomcat 自動配置
    │   │   ├── ViewResolverAutoConfiguration.java   # 視圖解析器自動配置
    │   │   └── WebAutoConfiguration.java            # Web 自動配置
    │   ├── base/controller/
    │   │   └── BaseController.java                  # 基礎 Controller
    │   ├── config/
    │   │   ├── JspConfig.java                       # JSP 設定
    │   │   ├── ThymeleafConfig.java                 # Thymeleaf 設定
    │   │   ├── WebPropertyConfig.java               # Web 屬性設定
    │   │   └── WebResourceConfig.java               # 靜態資源設定
    │   ├── controller/
    │   │   ├── RestfulController.java               # REST API Controller (範例)
    │   │   └── WebController.java                   # Web 頁面 Controller (範例)
    │   ├── dto/
    │   │   ├── Result.java                          # 統一回應 DTO
    │   │   └── User.java                            # 使用者 DTO
    │   ├── enums/
    │   │   └── ResultStatus.java                    # 回應狀態 Enum
    │   ├── exception/
    │   │   ├── IResultStatus.java                   # 回應狀態介面
    │   │   └── ResultException.java                 # 自訂例外
    │   └── util/
    │       └── DateFormatter.java                   # 日期格式化工具
    └── webapp/WEB-INF/
        ├── html/hello.html                          # HTML 靜態頁面
        ├── jsp/
        │   ├── hello.jsp                            # JSP 頁面
        │   └── test.jsp                             # JSP 測試頁面
        └── th/
            ├── message.html                         # Thymeleaf 頁面
            └── test.html                            # Thymeleaf 測試頁面
```

## 主要功能

JSP / Thymeleaf 視圖引擎切換、統一 REST 回應格式 (`@ResponseResultBody`)、基礎 Controller、靜態資源配置、Tomcat 嵌入式伺服器設定

## 對應文件

本模組的完整使用說明與開發指南請參閱 doc-site 技術文件：

- [模組簡介](../../doc-site/docs/web-starter/index.md)
- [快速開始](../../doc-site/docs/web-starter/quickstart.md)
- [配置參考](../../doc-site/docs/web-starter/configuration.md)
- [使用範例](../../doc-site/docs/web-starter/examples.md)
- [架構與開發指南](../../doc-site/docs/web-starter/architecture.md)
