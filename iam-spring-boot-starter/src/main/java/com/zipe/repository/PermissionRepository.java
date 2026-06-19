package com.zipe.repository;

import com.zipe.entity.Permission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 權限（{@link Permission}）的 Spring Data JPA Repository。
 * <p>
 * 提供權限的基本 CRUD 操作，並額外提供以權限代碼查詢的方法，供權限管理、
 * 唯一性檢查與查詢使用。
 * </p>
 *
 * @author Gary.Tsai
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 依權限代碼查詢權限資料。
     *
     * @param code 權限代碼
     * @return 對應的權限（以 {@link Optional} 包裝，查無時為空）
     */
    Optional<Permission> findByCode(String code);
}
