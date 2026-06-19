package com.zipe.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * BASIC 模式使用者設定區塊，對應 {@code security.basic.*}。
 *
 * <p>當 {@link #users} 有設定時，{@link com.zipe.service.BasicUserServiceImpl} 改由此清單
 * 載入帳號；未設定時 fallback 回內建的 {@code admin/admin}（向後相容）。</p>
 */
@Data
public class BasicUserPropertyConfig {

    /** 可登入的使用者清單，預設為空清單（fallback 至內建 admin/admin）。 */
    private List<BasicUser> users = new ArrayList<>();
}
