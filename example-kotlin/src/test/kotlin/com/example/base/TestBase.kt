package com.example.base

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.context.SpringBootTest

/**
 * 整合測試共用基底：載入完整 Spring context 並掛上 Kotest 的 [SpringExtension]，
 * 讓子類別測試可直接使用建構子注入的 Spring Bean。
 *
 * 對應 Java 來源的 `@SpringBootTest` 抽象基底（原以 JUnit 執行），
 * 改以 Kotest [FunSpec] 為基底，並透過 `extension(SpringExtension)` 整合 Spring context。
 */
@SpringBootTest
abstract class TestBase(body: FunSpec.() -> Unit = {}) : FunSpec(body) {

    init {
        // 讓 Kotest 啟用 Spring context 並支援建構子／欄位注入
        extension(SpringExtension)
    }
}
