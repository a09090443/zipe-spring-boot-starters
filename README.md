# 🚀 Zipe Spring Boot Starters

[![Maven Central](https://img.shields.io/maven-central/v/io.github.a09090443/base-spring-boot-starter.svg)](https://mvnrepository.com/search?q=a09090443)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

客製化 Spring Boot Starters 專案集合，提供企業級應用開發所需的常用功能模組。

---

## 📦 模組清單

| 模組 | 版本 | 說明 |
|------|------|------|
| [base-spring-boot-starter](base-spring-boot-starter/README.md) | 3.5.14.0 | 基礎工具（加解密、文件、郵件等） |
| [db-spring-boot-starter](db-spring-boot-starter/README.md) | 3.5.14.0 | 動態多資料來源切換 |
| [job-spring-boot-starter](job-spring-boot-starter/README.md) | 3.5.14.0 | Quartz 排程任務管理 |
| [logon-spring-boot-starter](logon-spring-boot-starter/README.md) | 3.5.14.0 | Spring Security 登入認證 |
| [web-spring-boot-starter](web-spring-boot-starter/README.md) | 3.5.14.0 | JSP / Thymeleaf 視圖整合 |
| [web-service-spring-boot-starter](web-service-spring-boot-starter/README.md) | 3.5.14.0 | Apache CXF SOAP WebService |
| [keycloak-spring-boot-starter](keycloak-spring-boot-starter/README.md) | 2.4.4.1 | 嵌入式 Keycloak OAuth 伺服器 |

---

## 🛠 開發環境

| 項目 | 版本要求 |
|------|----------|
| **JDK** | OpenJDK 17+ |
| **Spring Boot** | 3.5.x |
| **Maven** | 3.8.4+ |

> 🛑 `keycloak-spring-boot-starter` 為 **LEGACY**，凍結於 Java 11 / Spring Boot 2.4.4 / Keycloak 13，
> **未支援也無法升級至 Spring Boot 4**（嵌入式 Keycloak Server 架構已被上游移除）。詳見其
> [README](keycloak-spring-boot-starter/README.md)。

---

## 🚀 快速開始

在 `pom.xml` 加入所需的 Starter 依賴，詳細設定請參閱各模組的 README：

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>base-spring-boot-starter</artifactId>
    <version>3.5.14.0</version>
</dependency>
```

### 依賴關係

```
base-spring-boot-starter（基礎層）
        │
        ├── db-spring-boot-starter
        ├── job-spring-boot-starter
        ├── logon-spring-boot-starter
        ├── web-spring-boot-starter
        ├── web-service-spring-boot-starter
        └── keycloak-spring-boot-starter
```

---

## 📂 範例專案

| 範例 | 說明 |
|------|------|
| [starters_example](starters_example/README.md) | 整合所有 Starter 的完整範例（Java） |
| [example-kotlin](example-kotlin/README.md) | 整合範例（Kotlin + Gradle） |
| [example-keycloak](example-keycloak/README.md) | Keycloak 嵌入式伺服器範例 |
| [example](example/README.md) | 工具類別單元測試範例 |

---

## 📬 聯絡資訊

- **作者**: Gary Tsai
- **Email**: zipe.daden@gmail.com
- **Maven Repository**: [mvnrepository.com](https://mvnrepository.com/search?q=a09090443)
- **GitHub**: [a09090443/zipe-spring-boot-starters](https://github.com/a09090443/zipe-spring-boot-starters)

---

## 📄 授權條款

本專案採用 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) 授權。
