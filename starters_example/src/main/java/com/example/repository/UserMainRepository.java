package com.example.repository;

import com.example.model.UserMain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 使用者主要資料的 JPA Repository 介面。
 *
 * <p>繼承 {@link JpaRepository}，提供 {@link UserMain} 實體的基本 CRUD 操作，
 * 並額外宣告依使用者名稱查詢的方法。</p>
 *
 * @author zipe
 */
@Repository
public interface UserMainRepository extends JpaRepository<UserMain, Integer> {

    /**
     * 依使用者名稱查詢對應的 {@link UserMain} 實體。
     *
     * @param name 欲查詢的使用者名稱
     * @return 符合名稱的 {@link UserMain} 物件；若查無資料則回傳 {@code null}
     */
    UserMain findUserByName(@Param("name") String name);
}
