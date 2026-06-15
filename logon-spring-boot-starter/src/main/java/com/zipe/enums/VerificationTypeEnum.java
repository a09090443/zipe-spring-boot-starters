package com.zipe.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 驗證類型列舉，用於區分登入認證所採用的驗證方式。
 * <p>
 * 搭配 {@code SecurityPropertyConfig} 的設定屬性使用，
 * 系統啟動時依設定值選擇對應的驗證流程（基本資料庫驗證、LDAP 目錄服務驗證、或自訂驗證）。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/16 下午 05:10
 **/
public enum VerificationTypeEnum {
    /** 使用資料庫帳號密碼進行基本表單驗證 */
    BASIC,
    /** 使用 LDAP 目錄服務進行驗證 */
    LDAP,
    /** 使用自訂驗證流程 */
    CUSTOM,
    /** 使用 JWT 無狀態 token 驗證 */
    JWT;

    /**
     * 依字串名稱（不區分大小寫）取得對應的列舉常數。
     *
     * @param value 驗證類型名稱字串，例如 {@code "BASIC"}、{@code "ldap"}
     * @return 對應的 {@link VerificationTypeEnum} 常數；若 {@code value} 為空白或找不到對應值則回傳 {@code null}
     */
    public static VerificationTypeEnum getEnum(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        for (VerificationTypeEnum type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }

        return null;
    }
}
