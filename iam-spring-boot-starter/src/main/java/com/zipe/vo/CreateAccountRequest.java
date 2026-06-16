package com.zipe.vo;

import lombok.Data;

/**
 * 建立帳號的請求物件。
 * <p>
 * 對應 {@link com.zipe.entity.Account} 的可由外部指定欄位。{@link #password} 為明文，
 * 服務層會以注入的 {@link org.springframework.security.crypto.password.PasswordEncoder}
 * 編碼後再儲存；LDAP 來源帳號可不帶密碼。
 * </p>
 *
 * @author Gary.Tsai
 */
@Data
public class CreateAccountRequest {

    /** 登入帳號（必填，全表唯一）。 */
    private String username;

    /** 明文密碼，將由服務層編碼後儲存；LDAP 來源帳號可為空。 */
    private String password;

    /** 顯示名稱。 */
    private String displayName;

    /** 是否啟用，未指定時預設為啟用。 */
    private Boolean enabled;

    /** 是否鎖定，未指定時預設為未鎖定。 */
    private Boolean locked;
}
