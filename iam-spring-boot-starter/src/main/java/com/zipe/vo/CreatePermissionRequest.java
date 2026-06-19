package com.zipe.vo;

import lombok.Data;

/**
 * 建立權限的請求物件。
 * <p>
 * 對應 {@link com.zipe.entity.Permission} 的可由外部指定欄位。{@link #code} 即作為
 * Spring Security authority 字串（如 {@code USER_CREATE}）。
 * </p>
 *
 * @author Gary.Tsai
 */
@Data
public class CreatePermissionRequest {

    /** 權限代碼（必填，全表唯一，作為 authority 字串）。 */
    private String code;

    /** 顯示名稱。 */
    private String name;

    /** 權限說明。 */
    private String description;

    /** 是否啟用，未指定時預設為啟用。 */
    private Boolean enabled;
}
