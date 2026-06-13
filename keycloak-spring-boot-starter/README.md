# keycloak-spring-boot-starter

嵌入式 Keycloak 身份認證伺服器模組，允許在 Spring Boot 應用中直接內嵌 Keycloak，無需獨立部署。

> 🛑 **LEGACY — 未支援 Spring Boot 4，已凍結維護。**
>
> 本模組為**嵌入式 Keycloak Server**，凍結於 **Keycloak 13.0.1 / Spring Boot 2.4.4 / Java 11**，
> **不在** 本專案 SB4 升級範圍內，且**無法**升級至 Spring Boot 4。原因：
> - Keycloak 自 **v17（2022-06）** 起放棄 WildFly runtime 改為 Quarkus，**嵌入式 Server 架構已被上游移除**，
>   現代 Keycloak（26.x）為 Quarkus-only 獨立伺服器，無法嵌入 Spring Boot。
> - Keycloak 13 + RESTEasy 3.15 + Infinispan 11 全為 **javax（Jakarta EE 8）**，與 SB4 的
>   Jakarta EE 11 根本衝突，且無 Jakarta EE 11 版的嵌入式 Server 堆疊。
>
> **若需在 Spring Boot 4 整合 Keycloak**：請將 Keycloak 以獨立 Quarkus 伺服器部署，
> Spring Boot 端改用 **Spring Security OAuth2 / OIDC**（client 或 resource server）。此為另一項新開發，
> 非本模組之延續。

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
