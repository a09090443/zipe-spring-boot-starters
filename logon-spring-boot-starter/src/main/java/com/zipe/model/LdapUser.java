package com.zipe.model;

import lombok.Data;

/**
 * 代表從 LDAP 目錄服務查詢回來的使用者資料模型。
 * <p>
 * 此模型封裝了使用者在 LDAP 中的基本屬性，供登入驗證流程（{@code LdapUserDetailsService}）
 * 讀取並轉換為 Spring Security 的 {@code UserDetails} 物件使用。
 * </p>
 *
 * @author gary.tsai 2019/6/28
 */
@Data
public class LdapUser {

    /** 使用者帳號（對應 LDAP 的 uid 或 sAMAccountName 屬性）。 */
    private String userId;

    /** 使用者顯示名稱（對應 LDAP 的 cn 或 displayName 屬性）。 */
    private String name;

    /** 使用者電子郵件（對應 LDAP 的 mail 屬性）。 */
    private String email;

    /** 使用者在 LDAP 目錄樹中的完整識別名稱（Distinguished Name，DN）。 */
    private String ldapDn;

    /**
     * 帳號是否啟用的旗標。
     * <p>
     * 通常對應 LDAP 的 {@code userAccountControl} 或自訂屬性，
     * 值為 {@code "1"} 表示啟用，{@code "0"} 表示停用。
     * </p>
     */
    private String isEnabled;
}
