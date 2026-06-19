package com.example.service

import com.example.base.TestBase
import io.kotest.matchers.shouldBe

/**
 * [DBExampleService] 整合測試：驗證可依名稱查得使用者主要資料。
 */
class DBExampleServiceTest(
    private val dbExampleService: DBExampleService,
) : TestBase({

    test("findUserMainByNameTest") {
        val userMain = dbExampleService.getUserMainByName("Gary")
        userMain?.name shouldBe "Gary"
    }
})
