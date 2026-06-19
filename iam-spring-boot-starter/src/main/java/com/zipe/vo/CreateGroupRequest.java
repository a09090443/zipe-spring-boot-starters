package com.zipe.vo;

import lombok.Data;

/**
 * 建立群組（即角色）的請求物件。
 * <p>
 * 對應 {@link com.zipe.entity.Group} 的可由外部指定欄位。{@link #code} 為群組代碼，
 * 登入後會套用 {@code iam.group.role-prefix} 轉換為 Spring Security authority。
 * </p>
 *
 * @author Gary.Tsai
 */
@Data
public class CreateGroupRequest {

    /** 群組代碼（必填，全表唯一）。 */
    private String code;

    /** 群組名稱。 */
    private String name;

    /** 群組說明。 */
    private String description;

    /** 是否啟用，未指定時預設為啟用。 */
    private Boolean enabled;
}
