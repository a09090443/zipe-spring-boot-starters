import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.12"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.4.32"
    kotlin("plugin.spring") version "1.4.32"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    maven {
        url = uri("https://raw.github.com/a09090443/zipe-spring-boot-starters/mvn-repo")
    }
}
extra["BaseZipeSpringStarterVersion"] = "2.5.12.1"
extra["WebZipeSpringStarterVersion"] = "2.5.12.1"
extra["LogonSpringStarterVersion"] = "2.5.12.1"
extra["DbSpringStarterVersion"] = "2.5.12.1"
extra["JobSpringStarterVersion"] = "2.5.12.1"
extra["WebServiceSpringStarterVersion"] = "2.5.12.1"
extra["KotestJunit5Version"] = "4.6.0"
extra["KotestExtensionsSpringVersion"] = "4.4.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.lowagie:itext:2.1.7")
    implementation("org.olap4j:olap4j:1.2.0")

    implementation("com.zipe:base-spring-boot-starter:${property("BaseZipeSpringStarterVersion")}")
    implementation("com.zipe:web-spring-boot-starter:${property("WebZipeSpringStarterVersion")}")
    implementation("com.zipe:logon-spring-boot-starter:${property("LogonSpringStarterVersion")}")
    implementation("com.zipe:db-spring-boot-starter:${property("DbSpringStarterVersion")}")
    implementation("com.zipe:job-spring-boot-starter:${property("JobSpringStarterVersion")}")
    implementation("com.zipe:web-service-spring-boot-starter:${property("WebServiceSpringStarterVersion")}")

    implementation("com.h2database:h2")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:${property("KotestJunit5Version")}")
    testImplementation("io.kotest:kotest-extensions-spring:${property("KotestExtensionsSpringVersion")}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
