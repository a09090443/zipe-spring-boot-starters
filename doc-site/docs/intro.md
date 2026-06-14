---
id: intro
title: 專案總覽
sidebar_position: 1
---

# Zipe Spring Boot Starters

`zipe-spring-boot-starters` 是一套自製的 Spring Boot Starter 集合專案，提供各種常用功能的自動配置模組（Auto Configuration），讓各業務系統能夠以最少的設定快速引入所需功能。每個模組皆為獨立的 Maven 專案，遵循 Spring Boot Starter 的設計慣例，引入依賴後即可透過 `application.yml` 完成設定，無須撰寫繁瑣的樣板程式碼。

本專案的設計目標是**降低重複開發成本**與**統一技術選型**，將郵件、加解密、資料庫動態切換、排程、登入認證、WebService、前端視圖、Keycloak 嵌入式服務等常見需求封裝為可重複使用的模組。

## 可用模組

下表列出本專案目前提供的所有 Starter 模組，點擊連結可前往各模組的詳細文件：

| 模組 | 功能說明 | 文件連結 |
|---|---|---|
| **base-spring-boot-starter** | 基本工具功能（郵件、加解密、文件處理、HTTP 等） | [前往文件](./base-starter/index.md) |
| **db-spring-boot-starter** | 資料庫動態連接與多資料來源切換 | [前往文件](./db-starter/index.md) |
| **job-spring-boot-starter** | 基於 Quartz 的排程管理 | [前往文件](./job-starter/index.md) |
| **logon-spring-boot-starter** | 登入認證（Spring Security / LDAP） | [前往文件](./logon-starter/index.md) |
| **web-service-spring-boot-starter** | 基於 Apache CXF 的 SOAP WebService | [前往文件](./web-service-starter/index.md) |
| **web-spring-boot-starter** | 前端顯示（JSP / Thymeleaf） | [前往文件](./web-starter/index.md) |

## 環境需求

在使用本專案的任何模組之前，請確認您的開發環境符合以下需求：

| 項目 | 需求版本 |
|---|---|
| **Java** | 17 以上（建議使用 JDK 17 LTS） |
| **Spring Boot** | 4.0.x |
| **建構工具** | Maven 3.8+ |

:::note 為什麼是 Java 17？
Spring Boot 3.x 系列已將最低 Java 版本要求提升至 17，並全面改用 Jakarta EE 9+ 命名空間（`jakarta.*`）。若您的專案仍使用 Java 8 或 `javax.*` 命名空間，請先完成升級再引入本套件。
:::

## 快速引入

本專案的各 Starter 並未發布至公開的 Maven Central，而是發布至**本地 Maven Repository**。引入流程分為兩步：先在本機安裝 Starter，再於業務專案的 `pom.xml` 中宣告依賴。

### 步驟一：安裝 Starter 至本地 Repository

進入欲使用的 Starter 目錄，執行 Maven 安裝指令：

```bash
# 以 base-spring-boot-starter 為例
cd base-spring-boot-starter
./mvnw clean install
```

執行成功後，該 Starter 的 JAR 與 POM 會被安裝至本機的 `~/.m2/repository` 目錄。

### 步驟二：在業務專案引入依賴

於業務專案的 `pom.xml` 中加入對應的依賴宣告：

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>base-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

完成後重新整理 Maven 依賴，即可在程式中使用該 Starter 提供的功能。

:::tip 本地安裝建議
若您需要同時使用多個 Starter，可在專案根目錄一次性安裝所有模組。建議搭配 CI/CD 流程，於建構前自動執行各 Starter 的 `mvnw clean install`，確保版本一致。

```bash
for module in base db job logon web web-service keycloak; do
  (cd ${module}-spring-boot-starter && ./mvnw clean install -DskipTests)
done
```
:::

## 整合範例

專案內附 `starters_example` 整合測試範例專案，示範如何同時引入多個 Starter 並協同運作。可透過以下指令啟動：

```bash
cd starters_example
./mvnw spring-boot:run
```

:::info 更多範例
除了 `starters_example` 之外，專案另提供 `example`（工具範例）、`example-keycloak`（Keycloak 範例）與 `example-kotlin`（Kotlin 範例）三個範例專案，可依需求參考。
:::
