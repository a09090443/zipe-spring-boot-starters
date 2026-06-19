package com.zipe.dto;

import lombok.Data;

/**
 * 使用者資料傳輸物件（DTO），用於封裝登入或身份驗證時所需的帳號與密碼資訊。
 * <p>
 * 透過 Lombok {@code @Data} 自動產生 getter、setter、{@code equals}、
 * {@code hashCode} 與 {@code toString} 方法。
 * </p>
 */
@Data
public class User {
    /** 使用者帳號（登入名稱）。 */
    public String username;

    /** 使用者密碼；傳輸時請確保已加密或經由 HTTPS 保護。 */
    public String password;
}
