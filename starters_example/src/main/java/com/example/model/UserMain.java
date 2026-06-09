package com.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

/**
 * 使用者主要資料實體，對應資料庫 {@code user_main} 資料表。
 * <p>
 * 儲存使用者的基本識別資訊（主鍵 ID 與名稱），
 * 作為整合範例中各 Repository 與 Service 層的核心資料模型。
 * </p>
 *
 * @author Gary.Tsai
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name="user_main")
public class UserMain {

    /** 使用者主鍵，由資料庫自動遞增產生。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 使用者名稱。 */
    private String name;

    /**
     * 比較兩個 {@code UserMain} 實例是否代表同一筆資料庫記錄。
     * <p>
     * 以資料庫 ID 作為相等性依據，避免使用 Lombok 預設以所有欄位比較的策略，
     * 同時透過 {@link Hibernate#getClass(Object)} 區分代理類別與真實類別，
     * 防止 Hibernate Proxy 造成誤判。
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
        // 使用 Hibernate.getClass() 而非 getClass()，確保 Proxy 物件也能正確比較
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        UserMain userMain = (UserMain) o;
        return getId() != null && Objects.equals(getId(), userMain.getId());
    }

    /**
     * 依據類別型別計算雜湊值。
     * <p>
     * 配合 {@link #equals(Object)} 僅使用 ID 比較的策略，
     * 此處以類別本身的雜湊值作為固定回傳值，
     * 確保在 ID 尚未賦值（transient 狀態）時仍符合 {@code equals/hashCode} 契約。
     * </p>
     *
     * @return 此類別的 {@code hashCode}
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
