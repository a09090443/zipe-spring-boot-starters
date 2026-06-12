---
description: keycloak-spring-boot-starter 的功能說明（doc-site 文件尚未建立）
paths:
  - keycloak-spring-boot-starter/**
  - example-keycloak/**
---

# keycloak-spring-boot-starter

Keycloak 嵌入式服務的 Starter，允許在 Spring Boot 應用中直接嵌入 Keycloak 身份認證伺服器（Undertow + Infinispan 快取）。

## 主要功能領域

嵌入式 Keycloak 服務啟動、Spring Boot 屬性整合（`keycloak.custom.*`）、Infinispan 快取支援、Undertow 請求過濾、Realm 匯入（singleFile / multipleFiles）。

## 設定屬性前綴（無 doc-site，供快速參照）

| 前綴 | 說明 |
|---|---|
| `keycloak.custom.server.keycloak-path` | Keycloak context path（預設 `/auth`） |
| `keycloak.custom.admin-user.*` | 管理員帳號建立設定（create-admin-user-enabled、username、password） |
| `keycloak.custom.migration.*` | Realm 匯入（import-location、import-provider） |
| `keycloak.custom.infinispan.config-location` | Infinispan 設定檔路徑（預設 `classpath:infinispan.xml`） |
| `keycloak.*` | 其他 Keycloak 原生屬性（由 `KeycloakProperties` 以 Map 形式接收） |

## doc-site 文件

本模組尚未建立 doc-site 技術文件頁面。新增功能時請同步在 `doc-site/docs/keycloak-starter/` 下建立對應文件（index / quickstart / configuration / examples / architecture）。
