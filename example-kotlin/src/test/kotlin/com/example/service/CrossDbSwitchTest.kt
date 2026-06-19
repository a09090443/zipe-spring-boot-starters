package com.example.service

import com.example.base.TestBase
import com.example.repository.UserMainRepository
import com.zipe.base.database.DataSourceHolder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * 驗證 db-spring-boot-starter 可在「不同資料庫類型」之間動態切換（MySQL ↔ PostgreSQL）。
 *
 * example1 為 MySQL（僅有 `OnlyExample1`）、postgres 為 PostgreSQL（僅有 `OnlyPostgres`），
 * 兩者互無對方的獨有資料。透過 [DataSourceHolder] 切換後查詢，
 * 若切到 MySQL 只查得到 OnlyExample1、切到 PostgreSQL 只查得到 OnlyPostgres，
 * 即可證明查詢確實跨資料庫類型路由，而非 fallback 回 primary。
 */
class CrossDbSwitchTest(
    private val userMainRepository: UserMainRepository,
) : TestBase({

    // 每個測試後清除 ThreadLocal，避免資料來源狀態外洩到其他測試
    afterTest {
        DataSourceHolder.clearDataSourceName()
    }

    test("switchBetweenMysqlAndPostgres") {
        // 切換至 example1（MySQL）：只查得到 MySQL 的獨有資料
        DataSourceHolder.setDataSourceName("example1")
        userMainRepository.findUserByName("OnlyExample1")
            .shouldNotBeNull() // 切到 MySQL(example1) 後應查得到 OnlyExample1
        userMainRepository.findUserByName("OnlyPostgres")
            .shouldBeNull() // 切到 MySQL(example1) 後不應查得到 PostgreSQL 獨有的 OnlyPostgres

        // 切換至 postgres（PostgreSQL）：只查得到 PostgreSQL 的獨有資料
        DataSourceHolder.setDataSourceName("postgres")
        userMainRepository.findUserByName("OnlyPostgres")
            .shouldNotBeNull() // 切到 PostgreSQL(postgres) 後應查得到 OnlyPostgres
        userMainRepository.findUserByName("OnlyExample1")
            .shouldBeNull() // 切到 PostgreSQL(postgres) 後不應查得到 MySQL 獨有的 OnlyExample1
    }
})
