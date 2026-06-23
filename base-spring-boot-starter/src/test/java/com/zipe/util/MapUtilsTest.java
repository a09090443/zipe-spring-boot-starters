package com.zipe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link MapUtils#groupingBy_WithNullKeys} 支援 null 分組鍵且能正確聚合多值。
 */
class MapUtilsTest {

    /**
     * 一般分組：相同鍵的元素應聚合至同一 List。
     */
    @Test
    void groupsElementsBySameKey() {
        Map<Integer, List<String>> result = Stream.of("a", "bb", "cc", "ddd")
                .collect(MapUtils.groupingBy_WithNullKeys(String::length));

        assertEquals(List.of("a"), result.get(1));
        assertEquals(List.of("bb", "cc"), result.get(2));
        assertEquals(List.of("ddd"), result.get(3));
    }

    /**
     * 分類函式回傳 null 時不應拋出 NullPointerException，且 null 鍵能正常聚合多個元素。
     */
    @Test
    void supportsNullKeyWithoutThrowing() {
        Map<String, List<String>> result = Stream.of("apple", "ant", "bob", "x")
                .collect(MapUtils.groupingBy_WithNullKeys(s -> s.startsWith("a") ? "a" : null));

        assertEquals(2, result.get("a").size());
        assertTrue(result.containsKey(null));
        assertEquals(List.of("bob", "x"), result.get(null));
    }
}
