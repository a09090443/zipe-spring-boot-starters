package com.zipe.util.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link RandomUtil} 產生的隨機字串長度、字元集合與補零邏輯。
 */
class RandomUtilTest {

    /**
     * generateStr 應產生指定長度且字元全部落在英數字元集內。
     */
    @Test
    void generateStrHasCorrectLengthAndCharset() {
        String result = RandomUtil.generateStr(16);
        assertEquals(16, result.length());
        for (char c : result.toCharArray()) {
            assertTrue(RandomUtil.allChar.indexOf(c) >= 0, "非法字元: " + c);
        }
    }

    /**
     * generateLowerStr 應為純小寫字母，且等於自身的小寫形式。
     */
    @Test
    void generateLowerStrIsAllLowerCase() {
        String result = RandomUtil.generateLowerStr(20);
        assertEquals(20, result.length());
        assertEquals(result.toLowerCase(), result);
        for (char c : result.toCharArray()) {
            assertTrue(c >= 'a' && c <= 'z', "非小寫字母: " + c);
        }
    }

    /**
     * generateUpperStr 應為純大寫字母，且等於自身的大寫形式。
     */
    @Test
    void generateUpperStrIsAllUpperCase() {
        String result = RandomUtil.generateUpperStr(20);
        assertEquals(result.toUpperCase(), result);
        for (char c : result.toCharArray()) {
            assertTrue(c >= 'A' && c <= 'Z', "非大寫字母: " + c);
        }
    }

    /**
     * generateZeroStr 應全為字元 '0'。
     */
    @Test
    void generateZeroStrIsAllZeros() {
        assertEquals("00000", RandomUtil.generateZeroStr(5));
    }

    /**
     * toFixedLengthStr 應左補零至目標長度。
     */
    @Test
    void toFixedLengthStrLeftPadsWithZero() {
        assertEquals("000042", RandomUtil.toFixedLengthStr(42L, 6));
        assertEquals("123", RandomUtil.toFixedLengthStr(123L, 3));
    }

    /**
     * 數字位數超過目標長度時應拋出 RuntimeException。
     */
    @Test
    void toFixedLengthStrThrowsWhenNumberTooLong() {
        assertThrows(RuntimeException.class, () -> RandomUtil.toFixedLengthStr(123456L, 3));
    }
}
