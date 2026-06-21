package com.example.repository

import com.example.model.UserDetail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * UserDetail 資料存取介面。
 *
 * 繼承 [JpaRepository]，提供對 [UserDetail] 實體的基本 CRUD 操作，
 * 並額外定義依姓名與性別查詢的方法，由 Spring Data JPA 自動產生實作。
 *
 * @author zipe
 */
@Repository
interface UserDetailRepository : JpaRepository<UserDetail, String> {

    /**
     * 依姓名查詢使用者詳細資料。
     *
     * @param name 要查詢的使用者姓名
     * @return 符合姓名的 [UserDetail]，若不存在則回傳 `null`
     */
    fun findByName(name: String): UserDetail?

    /**
     * 依性別查詢使用者詳細資料。
     *
     * @param gender 要查詢的性別值
     * @return 符合性別的 [UserDetail]，若不存在則回傳 `null`
     */
    fun findByGender(gender: String): UserDetail?
}
