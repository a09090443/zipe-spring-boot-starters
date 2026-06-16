package com.zipe.repository;

import com.zipe.entity.Group;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 群組（{@link Group}）的 Spring Data JPA Repository。
 * <p>
 * 提供群組的基本 CRUD 操作，並額外提供以群組代碼查詢的方法，供群組管理、
 * 唯一性檢查與群組／權限掛卸使用。
 * </p>
 *
 * @author Gary.Tsai
 */
public interface GroupRepository extends JpaRepository<Group, Long> {

    /**
     * 依群組代碼查詢群組資料。
     *
     * @param code 群組代碼
     * @return 對應的群組（以 {@link Optional} 包裝，查無時為空）
     */
    Optional<Group> findByCode(String code);
}
