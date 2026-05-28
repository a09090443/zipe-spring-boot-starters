# keycloak-spring-boot-starter

嵌入式 Keycloak 身份認證伺服器模組，允許在 Spring Boot 應用中直接內嵌 Keycloak，無需獨立部署。

> **注意：** 此模組使用 Spring Boot **2.4.4** 與 **Java 11**，與其他 Starter 版本不同。

## 主要功能

- 嵌入式 Keycloak 服務啟動
- Spring Boot 自動配置整合
- Infinispan 快取支援
- Undertow 高效能 Web 容器
- RESTEasy JAX-RS 整合

## 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>keycloak-spring-boot-starter</artifactId>
    <version>2.4.4.1-SNAPSHOT</version>
</dependency>
```

## 基本設定

```properties
# Keycloak 伺服器監聽埠
keycloak.server.port=8180

# Realm 設定檔路徑
keycloak.server.keycloak-path=/keycloak
```

## 參考專案

本模組參考 [thomasdarimont/embedded-spring-boot-keycloak-server](https://github.com/thomasdarimont/embedded-spring-boot-keycloak-server)
