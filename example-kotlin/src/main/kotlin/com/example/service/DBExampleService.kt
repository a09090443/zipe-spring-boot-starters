package com.example.service

import com.example.model.UserDetail
import com.example.model.UserMain

/**
 * 資料庫範例服務介面。
 *
 * 定義以使用者名稱查詢 [UserMain] 與 [UserDetail] 的基本操作，
 * 示範透過 db-spring-boot-starter 進行多資料來源存取的使用方式。
 */
interface DBExampleService {

    /**
     * 依使用者名稱查詢使用者主要資料。
     *
     * @param name 使用者名稱
     * @return 對應的 [UserMain] 物件；若查無資料則回傳 `null`
     */
    fun getUserMainByName(name: String): UserMain?

    /**
     * 依使用者名稱查詢使用者詳細資料。
     *
     * @param name 使用者名稱
     * @return 對應的 [UserDetail] 物件；若查無資料則回傳 `null`
     */
    fun getUserDetailByName(name: String): UserDetail?
}
