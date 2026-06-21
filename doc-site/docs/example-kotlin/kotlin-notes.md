---
id: kotlin-notes
title: Kotlin 化重點
sidebar_position: 3
description: example-kotlin 與 starters_example 在語言、建構工具、測試框架與寫法上的差異
---

# Kotlin 化重點

本頁說明 `example-kotlin` 相對於 [`starters_example`](../integration/index.md) 在**語言與建構層面**的差異。兩者的功能、套件結構、端點與方法簽章一致；差異僅在於 Kotlin 的慣用寫法與 Gradle 設定。

## 與 starters_example 的差異對照

| 面向 | starters_example | example-kotlin |
|---|---|---|
| 語言 | Java 17 | Kotlin 2.2.x |
| 建構工具 | Maven（`pom.xml`、`mvnw`） | Gradle Kotlin DSL（`build.gradle.kts`、`gradlew`） |
| 測試框架 | JUnit 5 + Spring Test | Kotest（`FunSpec` + `kotest-extensions-spring` 的 `SpringExtension`） |
| 樣板簡化 | Lombok（`@Slf4j`、`@Data` 等） | Kotlin 慣例（`val/var`、字串模板、單運算式函式），不使用 Lombok |
| Null 安全 | 由 Java 慣例處理 | Kotlin 型別系統強制（`-Xjsr305=strict`） |
| JPA Entity | 標準 Java class | Kotlin `class` + `var` 屬性（`kotlin("plugin.jpa")` no-arg 外掛） |

## build.gradle.kts 重點

```kotlin
plugins {
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"   // 對 @Configuration/@Component 等套用 all-open
    kotlin("plugin.jpa") version "2.2.0"      // 對 @Entity 產生無參建構子（no-arg）
}

java {
    // 不鎖定 toolchain languageVersion，改由執行 Gradle 的 JDK（17 以上）編譯，
    // 避免在僅安裝較新 JDK 的環境上找不到 17 toolchain。
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    mavenLocal()   // 供解析本地 mvn install 的 4.0.0.1 Starter
}
```

- **七個 Starter** 以 `io.github.a09090443:*-spring-boot-starter:4.0.0.1` 引入。
- `kotlin-reflect` 為 Spring + Kotlin 必備。
- JSP 編譯所需的 `tomcat-embed-jasper` 以 `compileOnly` 引入（對應 Maven 的 `provided`）；H2 以 `runtimeOnly`；devtools 以 `developmentOnly`。PostgreSQL 等 JDBC 驅動已由 `db-spring-boot-starter` 內建提供，無須在此宣告。

## JPA Entity 寫法

Entity 採一般 `class` + `var` 屬性（非 `data class`），以保留與 Java 來源相同的 Hibernate 風格自訂 `equals`/`hashCode` 策略；`kotlin("plugin.jpa")` 會為其產生 JPA 所需的無參建構子。

```kotlin
@Entity
@Table(name = "user_main")
class UserMain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    var name: String? = null
    var gender: String? = null
    // equals/hashCode 沿用 Hibernate 最佳實務（以 Hibernate.getClass 區分代理）
}
```

## 測試（Kotest）

測試以 Kotest 的 `FunSpec` 撰寫，並透過 `kotest-extensions-spring` 的 `SpringExtension` 整合 Spring context；需要注入 Bean 的測試以「主建構子參數注入 + spec body lambda」表達，等同 Java 的 `@Autowired` 建構子注入。

```kotlin
abstract class TestBase(body: FunSpec.() -> Unit = {}) : FunSpec(body) {
    init { extension(SpringExtension) }
}

@SpringBootTest
@AutoConfigureMockMvc
class RestControllerTest(private val mockMvc: MockMvc) : TestBase({
    test("sayHello 回應問候字串") {
        mockMvc.perform(get("/rest/sayHello").param("name", "John").with(httpBasic("user01", "1234")))
            .andExpect(status().isOk)
    }
})
```

- Controller 測試沿用 `spring-security-test` 的 `httpBasic(...)` 帶認證身分，與 Java 版測試的驗證意圖一致。
- 部分測試（多資料來源、SOAP 客戶端、Excel 匯出寫檔）與 `starters_example` 同樣依賴外部環境（資料庫、執行中的服務、檔案路徑），僅在具備該環境時才會完整通過。
