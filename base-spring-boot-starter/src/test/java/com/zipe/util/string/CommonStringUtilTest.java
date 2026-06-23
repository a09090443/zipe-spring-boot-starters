package com.zipe.util.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link CommonStringUtil} 的補位、截取、數字驗證與首字母轉換等邏輯。
 */
class CommonStringUtilTest {

    /**
     * addZero 不足長度時左補零；位數已達或超過時原值回傳，不截斷。
     */
    @Test
    void addZeroPadsButNeverTruncates() {
        assertEquals("005", CommonStringUtil.addZero(5, 3));
        assertEquals("123", CommonStringUtil.addZero(123, 3));
        assertEquals("1234", CommonStringUtil.addZero(1234, 3));
    }

    /**
     * isNumber 接受數字與小數點，但拒絕負號與其他字元。
     */
    @Test
    void isNumberAcceptsDigitsAndDotOnly() {
        assertTrue(CommonStringUtil.isNumber("123.45"));
        assertFalse(CommonStringUtil.isNumber("-1"));
        assertFalse(CommonStringUtil.isNumber("12a"));
    }

    /**
     * isInteger 僅接受純數字，拒絕小數點與負號。
     */
    @Test
    void isIntegerAcceptsDigitsOnly() {
        assertTrue(CommonStringUtil.isInteger("123"));
        assertFalse(CommonStringUtil.isInteger("1.2"));
        assertFalse(CommonStringUtil.isInteger("-1"));
    }

    /**
     * stringRightPad 在右側補空白至指定長度；超長時截斷。
     */
    @Test
    void stringRightPadFillsRightWithSpaces() {
        assertEquals("ab   ", CommonStringUtil.stringRightPad("ab", 5));
        assertEquals("abc", CommonStringUtil.stringRightPad("abcdef", 3));
    }

    /**
     * stringLeftPad 在左側補空白至指定長度。
     */
    @Test
    void stringLeftPadFillsLeftWithSpaces() {
        assertEquals("   ab", CommonStringUtil.stringLeftPad("ab", 5));
    }

    /**
     * stringLeftZero 在左側補零；空白輸入回傳全零字串。
     */
    @Test
    void stringLeftZeroFillsLeftWithZeros() {
        assertEquals("000ab", CommonStringUtil.stringLeftZero("ab", 5));
        assertEquals("00000", CommonStringUtil.stringLeftZero(null, 5));
    }

    /**
     * getSubstring 對純英文字串以索引截取；非法索引回傳空字串。
     */
    @Test
    void getSubstringHandlesRangeAndInvalidIndex() {
        assertEquals("bc", CommonStringUtil.getSubstring("abcdef", 1, 3));
        assertEquals("cdef", CommonStringUtil.getSubstring("abcdef", 2));
        assertEquals("", CommonStringUtil.getSubstring("abc", -1));
        assertEquals("", CommonStringUtil.getSubstring(null, 0, 2));
    }

    /**
     * fillWithSpace 不足長度時右補空白；null 視為空字串；超長時截斷。
     */
    @Test
    void fillWithSpacePadsTruncatesAndHandlesNull() {
        assertEquals("ab   ", CommonStringUtil.fillWithSpace("ab", 5));
        assertEquals("   ", CommonStringUtil.fillWithSpace(null, 3));
        assertEquals("abc", CommonStringUtil.fillWithSpace("abcdef", 3));
    }

    /**
     * lowerCaseForFirstLetter 將首字母轉小寫；null 與空字串回傳空字串。
     */
    @Test
    void lowerCaseForFirstLetterHandlesNullAndEmpty() {
        assertEquals("hello", CommonStringUtil.lowerCaseForFirstLetter("Hello"));
        assertEquals("", CommonStringUtil.lowerCaseForFirstLetter(null));
        assertEquals("", CommonStringUtil.lowerCaseForFirstLetter(""));
    }
}
