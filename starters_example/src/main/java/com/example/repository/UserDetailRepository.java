package com.example.repository;

import com.example.model.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * UserDetail 資料存取介面。
 *
 * <p>繼承 {@link JpaRepository}，提供對 {@link UserDetail} 實體的基本 CRUD 操作，
 * 並額外定義依姓名與性別查詢的方法，由 Spring Data JPA 自動產生實作。
 *
 * @author zipe
 */
@Repository
public interface UserDetailRepository extends JpaRepository<UserDetail, String> {

  /**
   * 依姓名查詢使用者詳細資料。
   *
   * @param name 要查詢的使用者姓名
   * @return 符合姓名的 {@link UserDetail}，若不存在則回傳 {@code null}
   */
  UserDetail findByName(String name);

  /**
   * 依性別查詢使用者詳細資料。
   *
   * @param gender 要查詢的性別值
   * @return 符合性別的 {@link UserDetail}，若不存在則回傳 {@code null}
   */
  UserDetail findByGender(String gender);
}
