package com.zipe.repository;

import com.zipe.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 帳號（{@link Account}）的 Spring Data JPA Repository。
 * <p>
 * 提供帳號的基本 CRUD，並額外提供以登入名稱查詢的方法，供認證流程與授權解析使用。
 * </p>
 *
 * @author Gary.Tsai
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * 依登入帳號查詢帳號資料。
     *
     * @param username 登入帳號
     * @return 對應的帳號（以 {@link Optional} 包裝，查無時為空）
     */
    Optional<Account> findByUsername(String username);
}
