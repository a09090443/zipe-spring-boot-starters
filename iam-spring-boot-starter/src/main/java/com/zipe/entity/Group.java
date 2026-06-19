package com.zipe.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

/**
 * 群組（即角色）實體，對應資料表 {@code iam_group}。
 * <p>
 * 群組的 {@code code} 會在登入後套用 {@code iam.group.role-prefix}（預設 {@code ROLE_}）
 * 轉換為 Spring Security 的 authority，供 {@code hasRole()} 判斷使用；
 * 並透過 {@code iam_group_permission} 關聯表與 {@link Permission} 形成多對多關係。
 * </p>
 *
 * @author Gary.Tsai
 */
@Getter
@Setter
@ToString(exclude = "permissions")
@Entity
@Table(name = "iam_group")
public class Group {

    /** 群組主鍵，由資料庫自動遞增產生。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 群組代碼，全表唯一，對應 Security authority（套用 role-prefix）。 */
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    /** 群組名稱。 */
    @Column(name = "name", length = 100)
    private String name;

    /** 群組說明。 */
    @Column(name = "description", length = 255)
    private String description;

    /** 是否啟用。 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    /** 群組所擁有的權限集合（多對多，對應 {@code iam_group_permission} 關聯表）。 */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "iam_group_permission",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    /**
     * 比較兩個 {@code Group} 是否代表同一筆資料庫記錄。
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
        Group group = (Group) o;
        return getId() != null && Objects.equals(getId(), group.getId());
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
