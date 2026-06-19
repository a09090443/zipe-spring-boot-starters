---
description: base-spring-boot-starter 的功能說明與 doc-site 文件導覽
paths:
  - base-spring-boot-starter/**
---

# base-spring-boot-starter

基本功能的 Starter，提供各種通用工具類別，是其他 Starter 的共用基礎層。

## 主要功能領域

郵件發送、加解密（AES/3DES/MD5/Base64）、Excel/JasperReport 文件處理、HTTP 請求（OkHttp）、字串/日期工具、Bean 轉換、Velocity 樣板、類別動態載入。

## doc-site 文件導覽

工作於本模組時，依需求閱讀對應文件：

| 需求 | doc-site 文件 |
|---|---|
| 了解模組整體功能與主要類別清單 | [index.md](../../doc-site/docs/base-starter/index.md) |
| 引入 Maven 依賴與基本設定步驟 | [quickstart.md](../../doc-site/docs/base-starter/quickstart.md) |
| 查詢 `mail.*` / `velocity.*` 設定屬性（鍵、型別、預設值） | [configuration.md](../../doc-site/docs/base-starter/configuration.md) |
| 查詢 `AesUtil`、`OkHttpUtil`、`DateTimeUtils`、`MailService`、`ExcelUtil` 等 API 用法與程式碼範例 | [examples.md](../../doc-site/docs/base-starter/examples.md) |
| 了解套件結構、自動配置原理、`Crypto` 策略模式、`ThreadPoolTaskExecutor` Bean 覆寫方式 | [architecture.md](../../doc-site/docs/base-starter/architecture.md) |
