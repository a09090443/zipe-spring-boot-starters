plugins {
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    // 目標 bytecode 為 Java 17；不鎖定 toolchain languageVersion，
    // 改由執行 Gradle 的 JDK（17 以上）編譯，避免在僅安裝較新 JDK 的環境上找不到 17 toolchain。
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    mavenLocal()
}

extra["zipeSpringStarterVersion"] = "4.0.0.1"
extra["webjarsBootstrapVersion"] = "5.3.8"
extra["webjarsJqueryVersion"] = "3.7.1"
extra["webjarsLocatorVersion"] = "0.46"

dependencies {
    // zipe-spring-boot-starters（七個 starter）
    implementation("io.github.a09090443:base-spring-boot-starter:${property("zipeSpringStarterVersion")}")
    implementation("io.github.a09090443:web-spring-boot-starter:${property("zipeSpringStarterVersion")}")
    implementation("io.github.a09090443:web-service-spring-boot-starter:${property("zipeSpringStarterVersion")}")
    implementation("io.github.a09090443:job-spring-boot-starter:${property("zipeSpringStarterVersion")}")
    implementation("io.github.a09090443:db-spring-boot-starter:${property("zipeSpringStarterVersion")}")
    implementation("io.github.a09090443:logon-spring-boot-starter:${property("zipeSpringStarterVersion")}")
    implementation("io.github.a09090443:iam-spring-boot-starter:${property("zipeSpringStarterVersion")}")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Webjars（前端靜態資源）
    implementation("org.webjars:bootstrap:${property("webjarsBootstrapVersion")}")
    implementation("org.webjars:jquery:${property("webjarsJqueryVersion")}")
    implementation("org.webjars:webjars-locator:${property("webjarsLocatorVersion")}")

    // 編譯 JSP（內嵌 Tomcat Jasper）
    compileOnly("org.apache.tomcat.embed:tomcat-embed-jasper")

    // 資料庫驅動（PostgreSQL 等由 db-spring-boot-starter 內建提供，無須在此重複宣告）
    runtimeOnly("com.h2database:h2")

    // 開發工具
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // 測試
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.ow2.asm", module = "asm")
    }
    // SB4 將 MockMvc 測試切片（@AutoConfigureMockMvc）移至獨立工件
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    // 提供 @WithMockUser 等安全測試支援
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.9.1")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

tasks.test {
    useJUnitPlatform()
}
