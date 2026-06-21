package com.example.repository

import com.example.model.UserMain
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 使用者主要資料的 JPA Repository 介面。
 *
 * 繼承 [JpaRepository]，提供 [UserMain] 實體的基本 CRUD 操作，
 * 並額外宣告依使用者名稱查詢的方法。
 *
 * @author zipe
 */
@Repository
interface UserMainRepository : JpaRepository<UserMain, Int> {

    /**
     * 依使用者名稱查詢對應的 [UserMain] 實體。
     *
     * @param name 欲查詢的使用者名稱
     * @return 符合名稱的 [UserMain] 物件；若查無資料則回傳 `null`
     */
    fun findUserByName(@Param("name") name: String): UserMain?
}
