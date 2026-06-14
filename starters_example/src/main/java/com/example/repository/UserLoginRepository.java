package com.example.repository;

import com.example.model.UserLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 業務端登入帳號的 JPA Repository 介面。
 *
 * <p>繼承 {@link JpaRepository}，提供 {@link UserLogin} 實體的基本 CRUD 操作，
 * 供 CUSTOM 自訂登入流程依登入帳號查詢對應的密碼雜湊。</p>
 *
 * @author zipe
 */
@Repository
public interface UserLoginRepository extends JpaRepository<UserLogin, String> {

    /**
     * 依登入帳號查詢對應的 {@link UserLogin} 實體。
     *
     * @param loginId 欲查詢的登入帳號
     * @return 符合帳號的 {@link UserLogin} 物件；若查無資料則回傳 {@code null}
     */
    UserLogin findByLoginId(String loginId);
}
