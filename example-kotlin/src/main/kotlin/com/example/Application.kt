package com.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 應用程式進入點。
 *
 * 此類別為 Spring Boot 應用程式的啟動入口，透過 [SpringBootApplication] 啟用
 * 元件掃描、自動配置與 Spring Boot 全部預設功能，整合所有 zipe-spring-boot-starters
 * 模組的範例情境。
 *
 * 本應用自身的 JPA Repository（`com.example.repository`）改由 db-starter 的
 * `dynamic.base-packages` 設定（見 `data-source.properties`）驅動掃描，無須在此
 * 宣告 `@EnableJpaRepositories`。因 iam-starter 自帶 `@EnableJpaRepositories`
 * 會使 Spring Boot 對應用主套件的 Repository 自動掃描退讓，故以該設定重新啟用，與 iam 的
 * `com.zipe.repository` 各自獨立掃描、互不影響。
 *
 * @author Gary.tsai
 */
@SpringBootApplication
class Application

/**
 * 啟動 Spring Boot 應用程式。
 *
 * @param args 命令列引數，會由 Spring Boot 解析並對應至 `ApplicationArguments`
 */
fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
