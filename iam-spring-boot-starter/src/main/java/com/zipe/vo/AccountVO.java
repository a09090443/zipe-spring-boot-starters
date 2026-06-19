package com.zipe.vo;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Data;

/**
 * 帳號資料傳輸物件，作為服務層對外回傳的帳號視圖。
 * <p>
 * 刻意不外漏 {@link com.zipe.entity.Account} JPA 實體與其 lazy proxy；
 * 所屬群組僅以群組代碼字串集合（{@link #groupCodes}）呈現，避免在交易邊界外觸發
 * lazy loading。密碼雜湊基於資安考量不納入此視圖。
 * </p>
 *
 * @author Gary.Tsai
 */
@Data
public class AccountVO {

    /** 帳號主鍵。 */
    private Long id;

    /** 登入帳號。 */
    private String username;

    /** 顯示名稱。 */
    private String displayName;

    /** 是否啟用。 */
    private Boolean enabled;

    /** 是否鎖定。 */
    private Boolean locked;

    /** 建立時間。 */
    private LocalDateTime createdAt;

    /** 最後更新時間。 */
    private LocalDateTime updatedAt;

    /** 帳號所屬群組的代碼集合。 */
    private Set<String> groupCodes;
}
