package com.zipe.model;

import com.zipe.adapt.CdataAdapter;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.io.Serializable;

/**
 * SOAP WebService 範例使用者模型。
 *
 * <p>此類別作為 {@code UserService} WebService 介面的資料傳輸物件（DTO），
 * 透過 JAXB 的 {@code @XmlRootElement} 標記，使其可直接序列化為 XML 回應。
 * 其中 {@code email} 欄位透過 {@link CdataAdapter} 包裝為 CDATA 區段，
 * 以避免特殊字元在 XML 中造成解析問題。
 * </p>
 */
@XmlRootElement
public class User implements Serializable {
    /** 序列化版本識別碼，確保跨版本反序列化相容性。 */
    private static final long serialVersionUID = -3628469724795296287L;

    /** 使用者唯一識別碼。 */
    private String userId;
    /** 使用者顯示名稱。 */
    private String userName;
    /** 使用者電子郵件地址；序列化時以 CDATA 包裹，避免特殊字元破壞 XML 結構。 */
    private String email;


    /**
     * 取得使用者唯一識別碼。
     *
     * @return 使用者 ID 字串
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 設定使用者唯一識別碼。
     *
     * @param userId 使用者 ID 字串
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 取得使用者顯示名稱。
     *
     * @return 使用者名稱字串
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 設定使用者顯示名稱。
     *
     * @param userName 使用者名稱字串
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 取得使用者電子郵件地址。
     *
     * <p>此 getter 標記 {@code @XmlElement} 與 {@code @XmlJavaTypeAdapter(CdataAdapter.class)}，
     * 使 JAXB 序列化時將 email 值包裝為 {@code <![CDATA[...]]>} 區段，
     * 防止郵件地址中的 {@code &}、{@code <} 等字元干擾 XML 解析。
     * </p>
     *
     * @return 電子郵件地址字串
     */
    @XmlElement
    @XmlJavaTypeAdapter(CdataAdapter.class)
    public String getEmail() {
        return email;
    }

    /**
     * 設定使用者電子郵件地址。
     *
     * @param email 電子郵件地址字串
     */
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
