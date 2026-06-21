package com.zipe.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link VelocityUtil} 於 velocity-engine-core 2.x 下的 classpath 樣板渲染。
 *
 * <p>本測試為 velocity 1.7 → velocity-engine-core 2.4.1 升級的回歸安全網：
 * 確認既有以 1.x 風格設定的資源載入器 key 在 2.x 仍可正確載入並渲染樣板。</p>
 */
class VelocityUtilTest {

    /**
     * 以 classpath 載入器渲染 {@code velocity/hello.vm}，
     * 確認 {@code $name} 變數被正確替換為注入值。
     */
    @Test
    void generateContentRendersClasspathTemplate() {
        VelocityUtil velocityUtil = new VelocityUtil();
        velocityUtil.initClassPath();

        Map<String, Object> model = new HashMap<>();
        model.put("name", "Gary");

        String content = velocityUtil.generateContent("velocity/hello.vm", model);

        assertTrue(content.contains("Hello, Gary!"), "Velocity 應正確渲染英文變數");
        assertTrue(content.contains("你好，Gary。"), "Velocity 應以 UTF-8 正確渲染中文與變數");

        velocityUtil.close();
    }
}
