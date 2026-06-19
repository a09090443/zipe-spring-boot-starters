package com.example.service

import com.example.base.TestBase
import com.example.repository.UserMainRepository
import com.zipe.base.database.DataSourceHolder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * 驗證 db-spring-boot-starter 的多資料來源動態切換是否真正生效。
 *
 * example1 僅有 `OnlyExample1`、example2 僅有 `OnlyExample2`，兩者互無對方的獨有資料。
 * 透過 [DataSourceHolder] 切換後查詢，若切到 example1 只查得到 OnlyExample1、查不到 OnlyExample2，
 * 反之亦然，即可證明查詢確實被路由到不同資料來源，而非 fallback 回 primary。
 */
class DynamicDataSourceSwitchTest(
    private val userMainRepository: UserMainRepository,
) : TestBase({

    // 每個測試後清除 ThreadLocal，避免資料來源狀態外洩到其他測試
    afterTest {
        DataSourceHolder.clearDataSourceName()
    }

    test("switchBetweenExample1AndExample2") {
        // 切換至 example1：只查得到 example1 的獨有資料
        DataSourceHolder.setDataSourceName("example1")
        userMainRepository.findUserByName("OnlyExample1")
            .shouldNotBeNull() // 切到 example1 後應查得到 example1 獨有的 OnlyExample1
        userMainRepository.findUserByName("OnlyExample2")
            .shouldBeNull() // 切到 example1 後不應查得到 example2 獨有的 OnlyExample2（若查得到代表未真正切換）

        // 切換至 example2：只查得到 example2 的獨有資料
        DataSourceHolder.setDataSourceName("example2")
        userMainRepository.findUserByName("OnlyExample2")
            .shouldNotBeNull() // 切到 example2 後應查得到 example2 獨有的 OnlyExample2
        userMainRepository.findUserByName("OnlyExample1")
            .shouldBeNull() // 切到 example2 後不應查得到 example1 獨有的 OnlyExample1（若查得到代表未真正切換）
    }
})
