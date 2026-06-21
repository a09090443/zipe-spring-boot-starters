package com.example.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.util.Objects

/**
 * 使用者主要資料實體，對應資料庫 `user_main` 資料表。
 *
 * 儲存使用者的基本識別資訊（主鍵 ID 與名稱），
 * 作為整合範例中各 Repository 與 Service 層的核心資料模型。
 *
 * @author Gary.Tsai
 */
@Entity
@Table(name = "user_main")
class UserMain {

    /** 使用者主鍵，由資料庫自動遞增產生。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null

    /** 使用者名稱。 */
    var name: String? = null

    /**
     * 比較兩個 `UserMain` 實例是否代表同一筆資料庫記錄。
     *
     * 以資料庫 ID 作為相等性依據，並透過 [Hibernate.getClass] 區分代理類別與真實類別，
     * 防止 Hibernate Proxy 造成誤判。
     *
     * @param other 待比較的物件
     * @return 若兩者 ID 相同且均非 null 則回傳 `true`，否則回傳 `false`
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        // 使用 Hibernate.getClass() 而非 javaClass，確保 Proxy 物件也能正確比較
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false
        }
        other as UserMain
        return id != null && Objects.equals(id, other.id)
    }

    /**
     * 依據類別型別計算雜湊值。
     *
     * 配合 [equals] 僅使用 ID 比較的策略，
     * 此處以類別本身的雜湊值作為固定回傳值，
     * 確保在 ID 尚未賦值（transient 狀態）時仍符合 `equals/hashCode` 契約。
     *
     * @return 此類別的 `hashCode`
     */
    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "UserMain(id=$id, name=$name)"
}
