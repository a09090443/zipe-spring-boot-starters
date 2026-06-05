---
description: 專案總覽、技術規格與常用指令，適用於整個 zipe-spring-boot-starters 專案
---

# zipe-spring-boot-starters

自製的 Spring Boot Starter 集合專案，提供各種常用功能的自動配置模組，供各業務系統引入使用。

## 專案模組清單

| 模組目錄 | 功能說明 |
|---|---|
| `base-spring-boot-starter/` | 基本工具功能 |
| `db-spring-boot-starter/` | 資料庫動態連接 |
| `job-spring-boot-starter/` | Quartz 排程 |
| `logon-spring-boot-starter/` | 登入認證 (Spring Security / LDAP) |
| `web-service-spring-boot-starter/` | CXF WebService |
| `web-spring-boot-starter/` | 前端顯示 (JSP / Thymeleaf) |
| `keycloak-spring-boot-starter/` | Keycloak 嵌入式服務 |
| `starters_example/` | 整合測試範例專案 |
| `example/` | 測試工具範例 |
| `example-keycloak/` | Keycloak 範例 |
| `example-kotlin/` | Kotlin 範例 |

## 技術規格

- **Java 版本：** 17+
- **Spring Boot 版本：** 3.5.x
- **建構工具：** Maven
- **套件管理：** 各 Starter 獨立 `pom.xml`，發布至本地 Maven Repository

## 常用指令

```bash
# 建構並安裝單一 Starter 至本地 Maven Repository
cd <starter-directory>
./mvnw clean install

# 執行整合範例
cd starters_example
./mvnw spring-boot:run
```

## 對應文件

本模組的完整使用說明與開發指南請參閱 doc-site 技術文件：

- [專案總覽](../../doc-site/docs/intro.md)
