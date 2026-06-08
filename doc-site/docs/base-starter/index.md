---
id: index
title: base-spring-boot-starter
sidebar_position: 1
description: 提供郵件、加解密、文件處理、HTTP 與字串日期等通用工具的基礎 Starter
---

# base-spring-boot-starter

`base-spring-boot-starter` 是整個 Starter 集合的基礎模組，集中封裝了業務系統最常使用的通用工具類別。引入此模組後，您無需重複撰寫郵件發送、加解密、Excel 報表、HTTP 請求等樣板程式碼，即可透過注入的服務或靜態工具方法直接使用。

## 功能概述

本模組以「開箱即用」為設計原則，所有工具皆已透過 `BaseAutoConfiguration` 完成自動配置。郵件服務、執行緒池、Velocity 樣板等具狀態的元件會以 Spring Bean 形式註冊，而加解密、字串、日期等無狀態工具則以靜態方法提供。

## 主要特性

- **郵件發送**：透過 `MailService` 發送純文字、HTML 或帶附件的郵件，支援 Velocity 樣板套版。
- **加解密工具**：提供 AES、3DES、MD5、Base64、Hex 等多種加解密與編碼工具。
- **文件處理**：`ExcelUtil` 支援以 Annotation 方式匯入匯出 Excel；`JasperReportUtil` 支援報表輸出。
- **HTTP 請求**：`OkHttpUtil` 封裝 OkHttp，簡化 GET / POST 等 HTTP 呼叫。
- **字串與日期工具**：`CommonStringUtil`、`RandomUtil`、`DateTimeUtils` 處理常見字串與時間運算。
- **Bean 與類別載入**：`BeanUtil` 進行物件轉換，`JarClassLoader` 等支援動態類別載入。

## Maven 依賴引入

於業務專案的 `pom.xml` 加入以下依賴：

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>base-spring-boot-starter</artifactId>
    <version>3.5.11.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `base-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

下表列出本模組對外提供的核心類別與其職責：

| 類別 | 套件 | 職責 |
|---|---|---|
| `BaseAutoConfiguration` | `autoconfiguration` | 模組自動配置入口，無條件註冊核心 Bean |
| `MailService` / `MailServiceImpl` | `service` | 郵件發送介面與實作（五種發送方式） |
| `Mail` | `model` | 郵件資料模型（from / to / cc / bcc / subject / content / attachments） |
| `AesUtil` | `util/crypto` | AES-128/CBC/PKCS5Padding 加解密（每次隨機 IV，密文為 Base64(IV‖cipher)），支援字串與檔案 |
| `DESedeUtil` | `util/crypto` | 3DES/CBC/PKCS5Padding 加解密工具（每次隨機 IV，輸出為 Hex(IV‖cipher)）；3DES 屬淘汰演算法，新專案建議改用 `AesUtil` |
| `Md5Util` | `util/crypto` | MD5 雜湊（16/32 位、大小寫四種格式）；**已棄用**，禁止用於密碼或簽章 |
| `Base64Util` | `util/crypto` | Base64 編解碼工具 |
| `HexUtil` | `util/crypto` | byte[] ↔ Hex 字串互轉 |
| `CryptoUtil` | `util/crypto` | 策略模式加解密門面，委派給 `Crypto` 介面實作 |
| `ExcelUtil` | `util/doc` | Apache POI Excel 匯入 / 匯出（xls / xlsx、多 Sheet） |
| `JasperReportUtil` | `util/doc` | JasperReports PDF 產生工具 |
| `OkHttpUtil` | `util/http` | OkHttp 3 單例門面（同步 / 非同步 GET / POST） |
| `VelocityUtil` | `util` | Velocity 模板引擎（classpath / file / web 四種載入模式） |
| `BeanUtil` | `util/bean` | Bean 複製、JSON 序列化 / 反序列化（Gson + Jackson 雙引擎） |
| `DateTimeUtils` | `util/time` | Java 8 日期時間工具（18 個 Formatter、台灣民國年互轉） |
| `CommonStringUtil` | `util/string` | 數字格式化、補零、中文字串截取、首字母小寫 |
| `RandomUtil` | `util/string` | SecureRandom 亂數字串（英數 / 大小寫 / 定長） |
| `ApplicationContextHelper` | `util` | 讓非 Spring 元件從 Context 取得 Bean 的靜態工具 |
| `LdapUtil` | `util` | LDAP 連線、登入驗證、分頁搜尋使用者 / 群組 |
| `FileUtil` | `util/file` | Apache Commons IO 封裝（比較 / 查詢 / 讀寫 / 複製 / 刪除） |
| `PrintUtils` | `util/print` | Java AWT 本地列印（多頁、指定印表機、座標定位） |
| `RegexUtils` | `util/validation` | 靜態正規表達式驗證（Email / 手機 / 電話 / IP / URL） |
| `Validation` | `util/validation` | 正規表達式常數庫 + 台灣身分證字號演算法驗證 |
| `FileClassLoader` | `util/classloader` | 從目錄動態載入 `.class` 檔案 |
| `CustomClassLoader` | `util/classloader` | 動態載入外部 JAR，支援卸載（`unloadJarFile()`） |
| `JarClassLoader` | `util/classloader` | 指定套件強制從 JAR 重新載入（熱更新 / 外掛場景） |
| `MapUtils` | `util` | `groupingBy` 支援 null key 的 Stream Collector 工具 |
| `YamlPropertySourceFactory` | `util` | 讓 `@PropertySource` 能引用 `.yml` / `.yaml` 檔 |

## 快速導航

- [快速開始](./quickstart.md)：從零開始引入並驗證模組。
- [配置參考](./configuration.md)：完整的設定屬性與 `application.yml` 範例。
- [使用範例](./examples.md)：各項工具的實際程式碼範例。
- [架構與開發指南](./architecture.md)：模組設計原理、核心類別協作流程，以及擴充與維護指南。

:::tip 建議搭配
本模組為其他 Starter 的共用基礎，`db`、`logon`、`web` 等模組皆會間接依賴其工具類別，建議優先安裝。
:::
