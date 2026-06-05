---
description: web-service-spring-boot-starter 的目錄結構與功能說明
paths:
  - web-service-spring-boot-starter/**
---

# web-service-spring-boot-starter

WebService 功能的 Starter，基於 Apache CXF，提供 SOAP WebService 的服務端與客戶端整合。

## 目錄結構

```
web-service-spring-boot-starter/
└── src/main/java/com/zipe/
    ├── adapt/
    │   └── CdataAdapter.java                        # CDATA XML 轉換器
    ├── autoconfiguration/
    │   ├── CxfConfigAutoConfiguration.java          # CXF 框架自動配置
    │   └── WebServiceRegisterAutoConfiguration.java # WebService 自動註冊
    ├── config/
    │   ├── Service.java                             # 服務設定 Annotation
    │   └── WebServicePropertyConfig.java            # WebService 屬性設定
    ├── interceptor/
    │   ├── CdataContentInterceptor.java             # CDATA 內容攔截器 (入)
    │   └── ResponseCdataInterceptor.java            # CDATA 回應攔截器 (出)
    ├── model/
    │   └── User.java                                # 使用者模型 (範例)
    ├── service/
    │   ├── UserService.java                         # WebService 介面 (範例)
    │   └── impl/UserServiceImpl.java                # WebService 實作 (範例)
    └── util/
        ├── ClientLoginInterceptor.java              # 客戶端登入攔截器
        ├── SoapUtil.java                            # SOAP 訊息工具
        ├── WebServiceClientUtil.java                # WebService 客戶端工具
        └── XmlUtil.java                             # XML 處理工具
```

## 主要功能

CXF SOAP WebService 服務端自動註冊、WebService 客戶端呼叫工具、CDATA 內容攔截器、客戶端認證攔截器、XML 工具

## 對應文件

本模組的完整使用說明與開發指南請參閱 doc-site 技術文件：

- [模組簡介](../../doc-site/docs/web-service-starter/index.md)
- [快速開始](../../doc-site/docs/web-service-starter/quickstart.md)
- [配置參考](../../doc-site/docs/web-service-starter/configuration.md)
- [使用範例](../../doc-site/docs/web-service-starter/examples.md)
- [架構與開發指南](../../doc-site/docs/web-service-starter/architecture.md)
