# example-kotlin

`starters_example` 的 **Kotlin 對應版**，以 Kotlin + Gradle 1:1 重寫同一套整合範例，
展示 zipe-spring-boot-starters 各模組在 Kotlin 專案中的使用方式。功能與 `starters_example`
完全對齊（相同的類別、端點、Service 與測試情境），差別僅在語言、建構工具與測試框架。

- **語言：** Kotlin 2.2.x
- **平台：** Spring Boot 4.0.0 / Java 17（bytecode target 17）
- **建構工具：** Gradle（Kotlin DSL，`build.gradle.kts`）
- **Starter 版本：** `io.github.a09090443:*-spring-boot-starter:4.0.0.1`（七個 starter）

## 涵蓋情境

與 `starters_example` 相同，整合全部七個 starter：

- `base-spring-boot-starter`：加解密（CryptoUtil）、Excel 匯入匯出、JasperReport 報表、HTTP 工具
- `db-spring-boot-starter`：多資料來源動態切換（H2 記憶體資料庫）、BaseJDBC、SQL 外化
- `job-spring-boot-starter`：Quartz 排程（Annotation、DB、XML/Properties 三種設定方式）
- `logon-spring-boot-starter`：Spring Security 登入認證（含 JWT cookie 混合模式）
- `iam-spring-boot-starter`：帳號／群組／權限身分授權（與 db-starter 共用 EntityManagerFactory，
  以 `@PreAuthorize` 示範權限保護端點）
- `web-spring-boot-starter`：Thymeleaf 視圖、JSP 與統一回應格式
- `web-service-spring-boot-starter`：CXF SOAP WebService 服務端與客戶端

## 快速啟動

```bash
cd example-kotlin
./gradlew bootRun
```

啟動後可透過 `src/postman/Example.postman_collection.json` 匯入 Postman 進行 API 測試。

僅編譯（不啟動）：

```bash
./gradlew compileKotlin compileTestKotlin
```

執行測試（Kotest）：

```bash
./gradlew test
```

> 前置作業：本範例引用本地建構的 starter（`4.0.0.1`）。請先於專案根目錄執行
> `mvn -DskipTests install`，將七個 starter 安裝至本地 Maven Repository（`~/.m2`），
> Gradle 才能透過 `mavenLocal()` 解析到。

## 說明

- 資料庫使用 H2 記憶體資料庫，啟動時自動建表並載入初始資料（`src/main/resources/init/h2/`）
- 資料來源設定位於 `src/main/resources/data-source.properties`
- 排程設定位於 `src/main/resources/quartz-jobs.properties`
- iam 示範資料位於 `src/main/resources/init/iam-demo.sql`

## 與 starters_example 的差異

| 面向 | starters_example | example-kotlin |
|---|---|---|
| 語言 | Java 17 | Kotlin 2.2.x |
| 建構工具 | Maven（`pom.xml`、`mvnw`） | Gradle Kotlin DSL（`build.gradle.kts`、`gradlew`） |
| 測試框架 | JUnit 5 + Spring Test | Kotest（`FunSpec` + `kotest-extensions-spring` 的 `SpringExtension`）|
| 樣板簡化 | Lombok（`@Slf4j`、`@Data` 等） | Kotlin 慣例（`data class`、`val/var`、字串模板、單運算式函式），不使用 Lombok |
| Null 安全 | 由 Java 慣例處理 | Kotlin 型別系統強制（`-Xjsr305=strict`） |
| JPA Entity | 標準 Java class | Kotlin `class` + `var` 屬性（`kotlin("plugin.jpa")` no-arg 外掛產生無參建構子）|

> 套件結構（`com.example.*`）、類別名稱、方法簽章與對外端點皆與 `starters_example` 保持一致，
> 確保兩版範例可互相對照。
