---
id: quickstart
title: 快速啟動
sidebar_position: 2
description: 取得 Starter 依賴、以 Gradle 啟動 example-kotlin 並驗證各功能端點
---

# 快速啟動

## 環境需求

- **JDK 17 以上**（範例產出 Java 17 bytecode；`build.gradle.kts` 不鎖定 toolchain，由執行 Gradle 的 JDK 編譯，故安裝較新的 JDK 亦可）
- 內附 **Gradle 8.14 wrapper**，無須另行安裝 Gradle

## 步驟一：取得專案原始碼

```bash
git clone https://github.com/a09090443/zipe-spring-boot-starters.git
cd zipe-spring-boot-starters
```

## 步驟二：取得 Starter 依賴

`example-kotlin` 引用 `io.github.a09090443:*-spring-boot-starter:4.0.0.1`，`build.gradle.kts` 同時設定了 `mavenCentral()` 與 `mavenLocal()`。

- 若該版本已發布於 **Maven Central**，建構時會自動下載，無須額外步驟。
- 若要以**本地原始碼**建構最新版 Starter（例如剛改過 starter 程式碼），請先於專案根目錄將七個 Starter 安裝至本地 Maven Repository，Gradle 才能透過 `mavenLocal()` 解析：

```bash
mvn -DskipTests install
```

## 步驟三：啟動範例專案

```bash
cd example-kotlin
./gradlew bootRun
```

僅編譯（不啟動）：

```bash
./gradlew compileKotlin compileTestKotlin
```

執行測試（Kotest）：

```bash
./gradlew test
```

## 驗證端點

啟動成功後（context-path 為 `/example`，port `8080`），可透過以下端點驗證各功能：

| 功能 | 端點 |
|---|---|
| Thymeleaf 頁面 | `http://localhost:8080/example/thymeleaf` |
| JSP 頁面 | `http://localhost:8080/example/jsp` |
| REST API | `http://localhost:8080/example/rest/sayHello?name=John` |
| WebFlux SSE 串流 | `http://localhost:8080/example/rest/flux` |
| 多資料來源切換 API | `http://localhost:8080/example/rest/db/user?name=OnlyExample1&ds=example1`（換 `ds` 觀察切換，已放行免登入） |
| iam 授權解析示範 | `http://localhost:8080/example/iam-demo/authorities/alice` |
| iam 權限保護端點（需登入） | `http://localhost:8080/example/iam-demo/orders/export`（需 `ORDER_EXPORT`）、`/iam-demo/users/manage`（需 `USER_MANAGE`） |
| iam 內建帳號 API | `http://localhost:8080/example/api/iam/accounts` |
| SOAP WSDL | `http://localhost:8080/example/webservice/example?wsdl` |
| H2 Console | `http://localhost:8080/example/h2-console` |

可透過 `src/postman/Example.postman_collection.json` 匯入 Postman 進行 API 與 SOAP 測試。

:::tip 登入帳密與安全模式
範例預設 `security.verification-type: basic`，並以 `security.basic.users` 提供 `user01/1234`（具 `ORDER_EXPORT`）與 `user02/abcd`（另具 `USER_MANAGE`），可直接體驗 `@PreAuthorize` 權限保護端點。`security.jwt.enabled` 與 `example.hybrid-jwt.enabled` 預設皆為 `false`；設定方式與 `starters_example` 完全相同，詳見 [logon-starter 文件](../logon-starter/configuration.md)。
:::

:::note 資料庫與 SQL 初始化
與 `starters_example` 一致：內建 **H2 記憶體資料庫**並開啟 H2 Console；`spring.sql.init.mode` 設為 `never`，故 `init/schema.sql`、`init/data.sql` 與 `init/iam-demo.sql` **不會在啟動時自動執行**，需要時請自行於 H2 Console 載入。完整驗證多資料來源（`example1` / `example2`）時才需準備對應的資料庫環境。
:::
