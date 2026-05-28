---
description: keycloak-spring-boot-starter 的目錄結構與功能說明
paths:
  - keycloak-spring-boot-starter/**
  - example-keycloak/**
---

# keycloak-spring-boot-starter

Keycloak 嵌入式服務的 Starter，允許在 Spring Boot 應用中直接嵌入 Keycloak 身份認證伺服器。

## 目錄結構

```
keycloak-spring-boot-starter/
└── src/main/java/com/zipe/
    └── keycloak/
        ├── autoconfiguration/
        │   └── EmbeddedKeycloakAutoConfiguration.java  # Keycloak 嵌入式自動配置
        ├── scripting/
        │   └── EmbeddedScriptBasedComponentRegistrar.java  # 腳本元件註冊
        ├── support/
        │   ├── DynamicJndiContextFactoryBuilder.java   # 動態 JNDI 工廠
        │   ├── InfinispanCacheManagerProvider.java     # Infinispan 快取管理
        │   ├── KeycloakInitialContext.java              # Keycloak 初始化 Context
        │   ├── KeycloakUndertowRequestFilter.java      # Undertow 請求過濾器
        │   ├── PopulateKeycloakPropertiesApplicationListener.java  # 屬性初始化監聽器
        │   ├── Resteasy3Provider.java                  # RESTEasy 3 提供者
        │   ├── SpringBootConfigProvider.java           # Spring Boot 設定提供者
        │   └── SpringBootPlatformProvider.java         # Spring Boot 平台提供者
        ├── EmbeddedKeycloakApplication.java            # Keycloak 應用程式
        ├── EmbeddedKeycloakServer.java                 # Keycloak 伺服器
        ├── KeycloakCustomProperties.java               # 自訂 Keycloak 屬性
        └── KeycloakProperties.java                     # Keycloak 屬性設定
```

## 主要功能

嵌入式 Keycloak 服務啟動、Spring Boot 整合配置、Infinispan 快取支援、Undertow 請求過濾
