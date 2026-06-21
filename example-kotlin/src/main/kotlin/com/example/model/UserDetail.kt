package com.example.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.util.Objects

/**
 * 使用者詳細資料實體，對應資料庫中的 `user_detail` 資料表。
 *
 * 以使用者名稱（`name`）作為主鍵，記錄使用者的基本個人屬性，
 * 供 `UserDetailRepository` 進行 CRUD 操作。
 *
 * @author Gary.Tsai
 */
@Entity
@Table(name = "user_detail")
class UserDetail {

    /** 使用者名稱，同時作為主鍵（Primary Key）。 */
    @Id
    var name: String? = null

    /** 使用者性別。 */
    var gender: String? = null

    /**
     * 比較兩個 `UserDetail` 物件是否相等。
     *
     * 使用 [Hibernate.getClass] 取得實際代理類別，避免 Hibernate Proxy 與真實實體之間的比較誤判。
     * 僅在 `name` 不為 `null` 時才以 `name` 進行相等比較，
     * 確保尚未持久化（`name` 為 `null`）的實體不會互相判斷為相等。
     *
     * @param other 要比較的物件
     * @return 若兩物件屬於相同 Hibernate 類別且 `name` 相等則回傳 `true`，否則回傳 `false`
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false
        }
        other as UserDetail
        return name != null && Objects.equals(name, other.name)
    }

    /**
     * 回傳此實體的雜湊碼。
     *
     * 依 Hibernate 最佳實務，使用固定的 `javaClass.hashCode()` 作為雜湊值，
     * 確保實體在持久化前後（代理與非代理）能放入相同的集合（Set / Map）而不失效。
     *
     * @return 此實體類別的雜湊碼
     */
    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "UserDetail(name=$name, gender=$gender)"
}
