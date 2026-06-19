package com.zipe.vo;

import lombok.Data;

/**
 * 更新帳號的請求物件。
 * <p>
 * 僅涵蓋帳號的可變更基本資料；密碼變更請改用
 * {@link com.zipe.service.AccountService#changePassword(Long, String)}，
 * 群組異動請改用加入／移出群組相關方法。{@code null} 欄位視為不更新。
 * </p>
 *
 * @author Gary.Tsai
 */
@Data
public class UpdateAccountRequest {

    /** 顯示名稱，{@code null} 表示不更新。 */
    private String displayName;

    /** 是否啟用，{@code null} 表示不更新。 */
    private Boolean enabled;

    /** 是否鎖定，{@code null} 表示不更新。 */
    private Boolean locked;
}
