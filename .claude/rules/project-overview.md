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
| `example-keycloak/` | Keycloak 範例 |
| `example-kotlin/` | Kotlin 範例 |

## 技術規格

- **Java 版本：** 17+
- **Spring Boot 版本：** 3.5.x
- **建構工具：** Maven（多模組 reactor，根 `pom.xml` 為 parent 與聚合）
- **套件管理：** 6 個主要 Starter（base / db / job / logon / web / web-service）由根 `pom.xml` 統一管理版本與相依，發布至本地或遠端 Maven Repository；keycloak 與各 example 專案維持獨立

## 模組結構

- 根 `pom.xml`（`packaging=pom`）：繼承 `spring-boot-starter-parent`，集中 `groupId`、`version`、`dependencyManagement`、共用外掛與 `release` profile（javadoc / gpg / central-publishing）。
- 各 Starter 子 `pom.xml`：`<parent>` 指向根 pom，僅保留自身依賴（版本交由根 pom 的 `dependencyManagement` 管理）。
- 未納入 reactor：`keycloak-spring-boot-starter`、`starters_example`、`example` 系列。

## 常用指令

```bash
# 於專案根目錄一次建構並安裝所有 Starter 至本地 Maven Repository
mvn clean install

# 僅建構單一模組（含其相依模組）
mvn -pl db-spring-boot-starter -am clean install

# 發布（簽章 + 上傳 Maven Central），啟用 release profile
mvn -Prelease clean deploy

# 執行整合範例（範例專案獨立於 reactor 之外）
cd starters_example
./mvnw spring-boot:run
```

## 對應文件

本模組的完整使用說明與開發指南請參閱 doc-site 技術文件：

- [專案總覽](../../doc-site/docs/intro.md)
