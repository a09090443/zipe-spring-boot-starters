package com.zipe.vo;

import java.util.Set;
import lombok.Data;

/**
 * 群組（即角色）資料傳輸物件，作為服務層對外回傳的群組視圖。
 * <p>
 * 刻意不外漏 {@link com.zipe.entity.Group} JPA 實體與其 lazy proxy；
 * 所掛載的權限僅以權限代碼字串集合（{@link #permissionCodes}）呈現，
 * 避免在交易邊界外觸發 lazy loading。
 * </p>
 *
 * @author Gary.Tsai
 */
@Data
public class GroupVO {

    /** 群組主鍵。 */
    private Long id;

    /** 群組代碼（對應 Security authority，套用 role-prefix）。 */
    private String code;

    /** 群組名稱。 */
    private String name;

    /** 群組說明。 */
    private String description;

    /** 是否啟用。 */
    private Boolean enabled;

    /** 群組所擁有的權限代碼集合。 */
    private Set<String> permissionCodes;
}
