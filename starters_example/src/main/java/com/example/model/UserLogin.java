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
 * 業務端登入帳號實體，對應資料庫中的 {@code user_login} 資料表。
 * <p>
 * 此資料表專供 CUSTOM 自訂登入示範使用，與 {@link UserMain} / {@link UserDetail}
 * 各自獨立，避免影響多資料來源切換測試所依賴的 {@code user_main} 結構。
 * 以登入帳號（{@code loginId}）作為主鍵，{@code password} 欄位儲存 BCrypt 雜湊密碼。
 * </p>
 *
 * @author Gary.Tsai
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "user_login")
public class UserLogin {

    /** 登入帳號，同時作為主鍵（Primary Key）。 */
    @Id
    private String loginId;

    /** 登入密碼，儲存 BCrypt 雜湊字串（非明文）。 */
    private String password;

    /**
     * 比較兩個 {@code UserLogin} 物件是否代表同一筆登入帳號。
     * <p>
     * 使用 {@link Hibernate#getClass(Object)} 取得實際類別，避免 Hibernate Proxy
     * 與真實實體之間的比較誤判；僅在 {@code loginId} 不為 {@code null} 時以其進行比較。
     * </p>
     *
     * @param o 要比較的物件
     * @return 若兩物件屬於相同 Hibernate 類別且 {@code loginId} 相等則回傳 {@code true}，否則 {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        UserLogin that = (UserLogin) o;
        return getLoginId() != null && Objects.equals(getLoginId(), that.getLoginId());
    }

    /**
     * 回傳此實體的雜湊碼。
     * <p>
     * 依 Hibernate 最佳實務，使用固定的 {@code getClass().hashCode()} 作為雜湊值，
     * 確保實體在持久化前後（代理與非代理）能放入相同的集合而不失效。
     * </p>
     *
     * @return 此實體類別的雜湊碼
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
