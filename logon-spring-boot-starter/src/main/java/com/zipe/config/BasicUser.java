package com.zipe.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * BASIC 模式下單一可設定使用者，對應 {@code security.basic.users[]} 的元素。
 *
 * <p>提供帳號、密碼與權限三項設定，使 {@code verification-type=basic} 不再僅能使用
 * 內建的 {@code admin/admin} stub，而可由設定檔自訂多組帳號。</p>
 */
@Data
public class BasicUser {

    /** 登入帳號。 */
    private String username;

    /**
     * 登入密碼。支援兩種格式：
     * <ul>
     *   <li>明文（不以 {@code {} 開頭）：載入時自動以 {@code PasswordEncoder} 編碼。</li>
     *   <li>{@code {id}} 前綴的已雜湊值（例如 {@code {bcrypt}$2a$...}）：原值採用，交由
     *       {@code DelegatingPasswordEncoder} 依前綴比對。</li>
     * </ul>
     */
    private String password;

    /**
     * 權限清單，每個字串直接轉為 {@code SimpleGrantedAuthority}，不自動加 {@code ROLE_} 前綴。
     * 預設為空清單。
     */
    private List<String> authorities = new ArrayList<>();
}
