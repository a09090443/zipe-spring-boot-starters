package com.example.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.util.Objects

/**
 * 業務端登入帳號實體，對應資料庫中的 `user_login` 資料表。
 *
 * 此資料表專供 CUSTOM 自訂登入示範使用，與 [UserMain] / [UserDetail]
 * 各自獨立，避免影響多資料來源切換測試所依賴的 `user_main` 結構。
 * 以登入帳號（`loginId`）作為主鍵，`password` 欄位儲存 BCrypt 雜湊密碼。
 *
 * @author Gary.Tsai
 */
@Entity
@Table(name = "user_login")
class UserLogin {

    /** 登入帳號，同時作為主鍵（Primary Key）。 */
    @Id
    var loginId: String? = null

    /** 登入密碼，儲存 BCrypt 雜湊字串（非明文）。 */
    var password: String? = null

    /**
     * 比較兩個 `UserLogin` 物件是否代表同一筆登入帳號。
     *
     * 使用 [Hibernate.getClass] 取得實際類別，避免 Hibernate Proxy 與真實實體之間的比較誤判；
     * 僅在 `loginId` 不為 `null` 時以其進行比較。
     *
     * @param other 要比較的物件
     * @return 若兩物件屬於相同 Hibernate 類別且 `loginId` 相等則回傳 `true`，否則 `false`
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false
        }
        other as UserLogin
        return loginId != null && Objects.equals(loginId, other.loginId)
    }

    /**
     * 回傳此實體的雜湊碼。
     *
     * 依 Hibernate 最佳實務，使用固定的 `javaClass.hashCode()` 作為雜湊值，
     * 確保實體在持久化前後（代理與非代理）能放入相同的集合而不失效。
     *
     * @return 此實體類別的雜湊碼
     */
    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "UserLogin(loginId=$loginId, password=$password)"
}
