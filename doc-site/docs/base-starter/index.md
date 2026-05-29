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
    <groupId>com.zipe</groupId>
    <artifactId>base-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

:::note 安裝前置作業
引入前請先於 `base-spring-boot-starter` 目錄執行 `./mvnw clean install`，將模組安裝至本地 Maven Repository。
:::

## 主要類別

下表列出本模組對外提供的核心類別與其職責：

| 類別 | 職責 |
|---|---|
| `BaseAutoConfiguration` | 模組自動配置入口，註冊各項 Bean |
| `MailService` / `MailServiceImpl` | 郵件發送服務介面與實作 |
| `AesUtil` | AES 對稱加解密工具 |
| `DESedeUtil` | 3DES 加解密工具 |
| `Md5Util` | MD5 雜湊工具 |
| `Base64Util` | Base64 編解碼工具 |
| `ExcelUtil` | 以 Annotation 進行 Excel 匯入匯出 |
| `JasperReportUtil` | JasperReport 報表輸出工具 |
| `OkHttpUtil` | 基於 OkHttp 的 HTTP 請求工具 |
| `VelocityUtil` | Velocity 樣板渲染工具 |
| `CommonStringUtil` | 字串處理工具 |
| `DateTimeUtils` | 日期時間運算工具 |
| `BeanUtil` | Bean 物件轉換工具 |
| `ApplicationContextHelper` | 取得 Spring Context 與 Bean 的工具 |

## 快速導航

- [快速開始](./quickstart.md)：從零開始引入並驗證模組。
- [配置參考](./configuration.md)：完整的設定屬性與 `application.yml` 範例。
- [使用範例](./examples.md)：各項工具的實際程式碼範例。

:::tip 建議搭配
本模組為其他 Starter 的共用基礎，`db`、`logon`、`web` 等模組皆會間接依賴其工具類別，建議優先安裝。
:::
