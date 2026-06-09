package com.zipe.vo;

import java.io.Serial;
import lombok.Data;

import java.io.Serializable;

/**
 * 系統使用者 View Object，用於在登入流程中傳遞使用者基本資訊。
 * <p>
 * 主要在登入成功後，將使用者識別碼與登入時間封裝成此物件，
 * 供後續 Session 管理或登入記錄使用。
 * </p>
 *
 * @author gary.tsai 2019/5/31
 */
@Data
public class SysUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 使用者唯一識別碼 */
    private String userId;

    /** 使用者登入時間（格式由呼叫端決定） */
    private String loginTime;

}
