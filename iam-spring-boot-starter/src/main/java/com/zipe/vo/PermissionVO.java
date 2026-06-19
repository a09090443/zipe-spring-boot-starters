package com.zipe.vo;

import lombok.Data;

/**
 * 權限資料傳輸物件，作為服務層對外回傳的權限視圖。
 * <p>
 * 刻意不外漏 {@link com.zipe.entity.Permission} JPA 實體。權限為純粹的具名授權點，
 * 其 {@link #code} 即作為 Spring Security authority 字串使用。
 * </p>
 *
 * @author Gary.Tsai
 */
@Data
public class PermissionVO {

    /** 權限主鍵。 */
    private Long id;

    /** 權限代碼（作為 authority 字串，如 {@code USER_CREATE}）。 */
    private String code;

    /** 顯示名稱。 */
    private String name;

    /** 權限說明。 */
    private String description;

    /** 是否啟用。 */
    private Boolean enabled;
}
