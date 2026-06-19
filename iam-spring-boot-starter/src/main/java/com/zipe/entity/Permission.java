package com.zipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

/**
 * 權限實體，對應資料表 {@code iam_permission}。
 * <p>
 * 權限為純粹的「具名授權點」，刻意與 API／URL 解耦（不含 HTTP method 或 URL pattern）。
 * 其 {@code code} 會作為 Spring Security authority 字串（如 {@code USER_CREATE}），
 * 供 {@code hasAuthority()}、{@code @PreAuthorize} 或程式內手動判斷與選單顯示控制使用。
 * </p>
 *
 * @author Gary.Tsai
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "iam_permission")
public class Permission {

    /** 權限主鍵，由資料庫自動遞增產生。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 權限代碼，全表唯一，作為 authority 字串（如 {@code USER_CREATE}）。 */
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    /** 顯示名稱。 */
    @Column(name = "name", length = 100)
    private String name;

    /** 權限說明。 */
    @Column(name = "description", length = 255)
    private String description;

    /** 是否啟用。 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    /**
     * 比較兩個 {@code Permission} 是否代表同一筆資料庫記錄。
     * <p>
     * 以資料庫 ID 作為相等性依據，並透過 {@link Hibernate#getClass(Object)}
     * 區分代理類別與真實類別，避免 Hibernate Proxy 造成誤判。
     * </p>
     *
     * @param o 待比較的物件
     * @return 若兩者 ID 相同且均非 null 則回傳 {@code true}，否則回傳 {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Permission that = (Permission) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    /**
     * 回傳此實體的雜湊碼。
     * <p>
     * 配合 {@link #equals(Object)} 僅以 ID 比較的策略，以類別本身的雜湊值作為固定回傳值，
     * 確保在 ID 尚未賦值（transient 狀態）時仍符合 {@code equals/hashCode} 契約。
     * </p>
     *
     * @return 此實體類別的雜湊碼
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
