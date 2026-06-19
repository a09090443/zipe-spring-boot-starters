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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

/**
 * 帳號實體，對應資料表 {@code iam_account}。
 * <p>
 * 儲存登入帳號的核心資料：登入名稱、密碼雜湊、顯示名稱與啟用／鎖定狀態，
 * 並透過 {@code iam_account_group} 關聯表與 {@link Group} 形成多對多關係。
 * LDAP 來源的帳號其 {@code password} 欄位不使用，僅以 {@code username} 與群組指派
 * 提供授權資料。
 * </p>
 *
 * @author Gary.Tsai
 */
@Getter
@Setter
@ToString(exclude = "groups")
@Entity
@Table(name = "iam_account")
public class Account {

    /** 帳號主鍵，由資料庫自動遞增產生。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登入帳號，全表唯一。 */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /** 密碼（BCrypt 雜湊）；LDAP 來源帳號此欄不使用。 */
    @Column(name = "password", length = 200)
    private String password;

    /** 顯示名稱。 */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /** 是否啟用，停用即不可登入。 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    /** 是否鎖定。 */
    @Column(name = "locked", nullable = false)
    private Boolean locked = Boolean.FALSE;

    /** 建立時間（稽核）。 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 最後更新時間（稽核）。 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 帳號所屬群組集合（多對多，對應 {@code iam_account_group} 關聯表）。 */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "iam_account_group",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> groups = new HashSet<>();

    /**
     * 比較兩個 {@code Account} 是否代表同一筆資料庫記錄。
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
        Account account = (Account) o;
        return getId() != null && Objects.equals(getId(), account.getId());
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
