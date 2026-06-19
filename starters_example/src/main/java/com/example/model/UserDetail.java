package com.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;


/**
 * 使用者詳細資料實體，對應資料庫中的 {@code user_detail} 資料表。
 * <p>
 * 以使用者名稱（{@code name}）作為主鍵，記錄使用者的基本個人屬性，
 * 供 {@code UserDetailRepository} 進行 CRUD 操作。
 * </p>
 *
 * @author Gary.Tsai
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name="user_detail")
public class UserDetail {

    /** 使用者名稱，同時作為主鍵（Primary Key）。 */
    @Id
    private String name;

    /** 使用者性別。 */
    private String gender;

    /**
     * 比較兩個 {@code UserDetail} 物件是否相等。
     * <p>
     * 使用 {@link Hibernate#getClass(Object)} 取得實際代理類別，
     * 避免 Hibernate Proxy 與真實實體之間的比較誤判。
     * 僅在 {@code name} 不為 {@code null} 時才以 {@code name} 進行相等比較，
     * 確保尚未持久化（{@code name} 為 {@code null}）的實體不會互相判斷為相等。
     * </p>
     *
     * @param o 要比較的物件
     * @return 若兩物件屬於相同 Hibernate 類別且 {@code name} 相等則回傳 {@code true}，否則回傳 {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        UserDetail that = (UserDetail) o;
        return getName() != null && Objects.equals(getName(), that.getName());
    }

    /**
     * 回傳此實體的雜湊碼。
     * <p>
     * 依 Hibernate 最佳實務，使用固定的 {@code getClass().hashCode()} 作為雜湊值，
     * 確保實體在持久化前後（代理與非代理）能放入相同的集合（Set / Map）而不失效。
     * </p>
     *
     * @return 此實體類別的雜湊碼
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
