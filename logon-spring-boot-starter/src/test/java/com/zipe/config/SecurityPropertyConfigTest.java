package com.zipe.config;

import com.zipe.enums.FrameOptionsMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 驗證安全性設定的安全預設值。
 */
class SecurityPropertyConfigTest {

    /**
     * frameOptions 預設必須為 SAMEORIGIN（保留點擊劫持防護），不可預設為 DISABLE。
     */
    @Test
    void frameOptionsDefaultsToSameOrigin() {
        SecurityPropertyConfig config = new SecurityPropertyConfig();
        assertEquals(FrameOptionsMode.SAMEORIGIN, config.getFrameOptionsMode());
    }
}
