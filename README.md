# 🚀 Zipe Spring Boot Starters

[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.a09090443/base-spring-boot-starter.svg)](https://mvnrepository.com/search?q=a09090443)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

客製化 Spring Boot Starters 專案集合，提供企業級應用開發所需的常用功能模組。

## 📋 目錄

- [專案列表](#-專案列表)
- [開發環境](#-開發環境)
- [快速開始](#-快速開始)
- [模組詳細說明](#-模組詳細說明)
  - [base-spring-boot-starter](#1-base-spring-boot-starter-基礎專案)
  - [db-spring-boot-starter](#2-db-spring-boot-starter-數據庫專案)
  - [job-spring-boot-starter](#3-job-spring-boot-starter-任務排程專案)
  - [logon-spring-boot-starter](#4-logon-spring-boot-starter-登入控制專案)
  - [web-spring-boot-starter](#5-web-spring-boot-starter-web-framework-專案)
  - [web-service-spring-boot-starter](#6-web-service-spring-boot-starter-cxf-webservices-專案)
  - [keycloak-spring-boot-starter](#7-keycloak-spring-boot-starter-oauth-專案)
- [範例專案](#-範例專案)
- [聯絡資訊](#-聯絡資訊)

---

## 📦 專案列表

| 模組名稱 | 版本 | 說明 |
|---------|------|------|
| `base-spring-boot-starter` | 3.2.5.4 | 基礎專案，提供通用工具類和服務 |
| `db-spring-boot-starter` | 3.2.5.3 | 動態數據源專案，支援多數據庫切換 |
| `job-spring-boot-starter` | 3.2.5.1 | Quartz 任務排程專案 |
| `logon-spring-boot-starter` | 3.2.5.1 | Spring Security 登入控制專案 |
| `web-spring-boot-starter` | 3.2.5.1 | Thymeleaf/JSP Web Framework 專案 |
| `web-service-spring-boot-starter` | 3.2.5.3 | Apache CXF WebServices 專案 |
| `keycloak-spring-boot-starter` | 2.4.4.1 | 嵌入式 Keycloak OAuth 專案 |

---

## 🛠 開發環境

| 項目 | 版本要求 |
|------|----------|
| **JDK** | OpenJDK 17+ |
| **Spring Boot** | 3.2.5+ |
| **Maven** | 3.8.4+ |

> ⚠️ 注意：`keycloak-spring-boot-starter` 需使用 Java 11 和 Spring Boot 2.4.4

---

## 🚀 快速開始

### Maven 依賴

在您的專案 `pom.xml` 中添加所需的 Starter：

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>base-spring-boot-starter</artifactId>
    <version>3.2.5.4</version>
</dependency>
```

### 依賴關係

```
base-spring-boot-starter (基礎層)
        │
        ├── db-spring-boot-starter
        ├── job-spring-boot-starter
        ├── logon-spring-boot-starter
        ├── web-spring-boot-starter
        ├── web-service-spring-boot-starter
        └── keycloak-spring-boot-starter
```

---

## 📖 模組詳細說明

### 1. base-spring-boot-starter (基礎專案)

所有其他 Starter 的基礎依賴模組，提供通用工具類和服務。

#### 核心功能

- **郵件服務** - 基於 Spring Boot Mail 的郵件發送服務
- **Velocity 模板引擎** - 動態文檔內容生成
- **線程池管理** - 預設配置的 `ThreadPoolTaskExecutor`
- **國際化支援** - 多語言訊息資源管理

#### 工具類庫

| 類別 | 工具類 |
|------|--------|
| 加密工具 | `AesUtil`, `Base64Util`, `CryptoUtil`, `DESedeUtil`, `HexUtil`, `Md5Util` |
| 文檔處理 | `ExcelUtil` (Apache POI), `JasperReportUtil` (報表生成) |
| 網路工具 | `OkHttpUtil` (HTTP 客戶端), `LdapUtil` (LDAP 操作) |
| 字串處理 | `CommonStringUtil`, `RandomUtil` |
| 日期時間 | `DateTimeUtils` |
| 文件處理 | `FileUtil` |
| 驗證工具 | `RegexUtils`, `Validation` |
| Bean 工具 | `BeanUtil`, 自訂序列化器 |

#### 使用方式

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>base-spring-boot-starter</artifactId>
    <version>3.2.5.4</version>
</dependency>
```

---

### 2. db-spring-boot-starter (數據庫專案)

動態數據源管理，支援運行時切換多個數據庫。

#### 核心功能

- **動態數據源切換** - 使用 `@DS` 註解切換數據源
- **多數據源支援** - 同時配置多個不同類型的數據庫
- **密碼加密** - 支援數據庫密碼加密儲存
- **JPA 整合** - 完整的 JPA/Hibernate 配置

#### 支援的數據庫

- MySQL (mysql-connector-j 8.4.0)
- MS SQL Server (mssql-jdbc 11.2.3)
- MariaDB (mariadb-java-client 3.1.3)
- AS/400 (jt400 11.2)

#### 使用方式

```java
// 在類別或方法上標記使用特定數據源
@DS("secondary")
public class UserRepository {
    // 使用 secondary 數據源
}

// 或在方法層級
@DS("primary")
public User findById(Long id) {
    // 使用 primary 數據源
}
```

#### 配置範例 (data-source.properties)

```properties
# 主數據源
dynamic.datasource.primary=master
dynamic.datasource.datasource-map.master.url=jdbc:mysql://localhost:3306/db1
dynamic.datasource.datasource-map.master.username=root
dynamic.datasource.datasource-map.master.pa55word=password

# 次要數據源
dynamic.datasource.datasource-map.secondary.url=jdbc:mysql://localhost:3306/db2
dynamic.datasource.datasource-map.secondary.username=root
dynamic.datasource.datasource-map.secondary.pa55word=password
```

---

### 3. job-spring-boot-starter (任務排程專案)

基於 Quartz 的排程任務管理模組。

#### 核心功能

- **Quartz 整合** - Spring Boot Quartz Starter 封裝
- **配置檔驅動** - 通過 `quartz-jobs.properties` 配置排程任務
- **REST API** - 提供任務管理 API
- **自動創建任務** - 應用啟動時自動創建配置的排程任務

#### 使用方式

```java
// 創建自訂 Job
public class MyJob extends BaseJob {
    @Override
    protected void executeInternal(JobExecutionContext context) {
        // 任務邏輯
    }
}
```

#### 配置範例 (quartz-jobs.properties)

```properties
quartz.job-map.job1.name=MyFirstJob
quartz.job-map.job1.clazz=com.example.job.MyJob
quartz.job-map.job1.cron-expression=0 0/5 * * * ?
```

---

### 4. logon-spring-boot-starter (登入控制專案)

Spring Security 認證整合模組，支援多種登入方式。

#### 核心功能

- **多種認證模式** - BASIC / LDAP / CUSTOM
- **Session 管理** - 支援多 Session 控制與過期處理
- **自訂登入頁面** - 可配置登入 URI
- **事件處理器** - 登入成功/失敗/登出事件處理

#### 認證模式

| 模式 | 說明 |
|------|------|
| `BASIC` | 基本認證，使用內建 `BasicUserServiceImpl` |
| `LDAP` | LDAP 認證，使用 `LdapUserDetailsService` |
| `CUSTOM` | 自訂認證，需配置自訂 Provider Bean 名稱 |

#### 配置範例 (application.properties)

```properties
# 啟用安全控制
security.enable=true

# 認證類型: BASIC, LDAP, CUSTOM
security.verification-type=BASIC

# 自訂登入頁面 (選填)
security.login-uri=/login

# 允許訪問的 URI (不需認證)
security.allow-uris=/public/**,/static/**

# CSRF 開關
security.csrf-enabled=false

# LDAP 配置 (當使用 LDAP 模式時)
ldap.url=ldap://localhost:389
ldap.base=dc=example,dc=com
```

---

### 5. web-spring-boot-starter (Web Framework 專案)

Thymeleaf 和 JSP 視圖整合模組。

#### 核心功能

- **Thymeleaf 整合** - 模板引擎配置
- **JSP 支援** - Tomcat Jasper、JSTL 標籤庫
- **靜態資源管理** - 可配置資源路徑
- **國際化支援** - 語言切換攔截器
- **統一響應格式** - `@ResponseResultBody` 註解自動封裝

#### 統一響應封裝

```java
@RestController
@ResponseResultBody  // 自動封裝所有響應
public class UserController {
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
        // 響應會自動封裝為 { "code": 200, "message": "success", "data": {...} }
    }
}
```

---

### 6. web-service-spring-boot-starter (CXF WebServices 專案)

Apache CXF SOAP WebService 整合模組。

#### 核心功能

- **WebService 自動發佈** - 配置驅動的服務註冊
- **CDATA 處理** - 特殊 XML 內容處理
- **WebService 客戶端工具** - 簡化客戶端呼叫
- **JAXB 資料綁定** - 自訂 JAXB 配置

#### 使用方式

1. 定義 WebService 介面和實作：

```java
@WebService
public interface UserService {
    User getUser(Long id);
}

@Service("userWebService")
public class UserServiceImpl implements UserService {
    public User getUser(Long id) {
        // 實作邏輯
    }
}
```

2. 配置發佈 (web-service.properties)：

```properties
web-service.map.user.bean-name=userWebService
web-service.map.user.uri-mapping=/ws/user
```

---

### 7. keycloak-spring-boot-starter (OAuth 專案)

嵌入式 Keycloak 伺服器模組。

> ⚠️ 注意：此模組使用 Spring Boot 2.4.4 和 Java 11

#### 核心功能

- **嵌入式 Keycloak** - 無需獨立部署 Keycloak 伺服器
- **Infinispan 快取** - 內建分散式快取
- **RESTEasy 整合** - JAX-RS 實現
- **Undertow 容器** - 高效能 Web 容器

#### 參考專案

本模組參考 [thomasdarimont/embedded-spring-boot-keycloak-server](https://github.com/thomasdarimont/embedded-spring-boot-keycloak-server)

---

## 📂 範例專案

本專案提供多個範例供參考：

| 範例專案 | 說明 |
|---------|------|
| `example` | 基本使用範例 |
| `example-keycloak` | Keycloak 整合範例 |
| `example-kotlin` | Kotlin 語言範例 |
| `starters_example` | 完整 Starter 使用範例 |

---

## 📬 聯絡資訊

- **作者**: Gary Tsai
- **Email**: zipe.daden@gmail.com
- **Maven Repository**: [mvnrepository.com](https://mvnrepository.com/search?q=a09090443)
- **GitHub**: [a09090443/zipe-spring-boot-starters](https://github.com/a09090443/zipe-spring-boot-starters)

如果您有興趣討論或想加入開發，歡迎與我聯絡！

---

## 📄 授權條款

本專案採用 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) 授權。
