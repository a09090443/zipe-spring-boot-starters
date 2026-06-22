package com.zipe.util.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link RegexUtils} 各格式驗證方法對合法／非法輸入的判斷。
 * <p>本測試固化修正量詞缺陷後的正確行為，避免 regex 再次退化。</p>
 */
class RegexUtilsTest {

    @Test
    void checkEmail() {
        assertTrue(RegexUtils.checkEmail("user@example.com"));
        assertTrue(RegexUtils.checkEmail("user@example.com.tw"));
        assertFalse(RegexUtils.checkEmail("invalid"));
        assertFalse(RegexUtils.checkEmail("a@b"));
    }

    @Test
    void checkMobile() {
        assertTrue(RegexUtils.checkMobile("13812345678"));
        assertTrue(RegexUtils.checkMobile("+8613812345678"));
        assertFalse(RegexUtils.checkMobile("12345"));
    }

    @Test
    void checkPhone() {
        assertTrue(RegexUtils.checkPhone("0212345678"));
        assertTrue(RegexUtils.checkPhone("12345678"));
        assertFalse(RegexUtils.checkPhone("abc"));
    }

    @Test
    void checkDigit() {
        assertTrue(RegexUtils.checkDigit("123"));
        assertTrue(RegexUtils.checkDigit("5"));
        assertTrue(RegexUtils.checkDigit("-7"));
        assertFalse(RegexUtils.checkDigit("abc"));
    }

    @Test
    void checkDecimals() {
        assertTrue(RegexUtils.checkDecimals("1.23"));
        assertTrue(RegexUtils.checkDecimals("233.30"));
        assertTrue(RegexUtils.checkDecimals("5"));
        assertFalse(RegexUtils.checkDecimals("abc"));
    }

    @Test
    void checkBlankSpace() {
        assertTrue(RegexUtils.checkBlankSpace("   "));
        assertFalse(RegexUtils.checkBlankSpace("a b"));
    }

    @Test
    void checkChinese() {
        assertTrue(RegexUtils.checkChinese("中文字"));
        assertFalse(RegexUtils.checkChinese("中a"));
        assertFalse(RegexUtils.checkChinese("abc"));
    }

    @Test
    void checkBirthday() {
        assertTrue(RegexUtils.checkBirthday("2024-09-03"));
        assertTrue(RegexUtils.checkBirthday("1992/09/03"));
        assertTrue(RegexUtils.checkBirthday("1992.09.03"));
        // 分隔符號必須前後一致
        assertFalse(RegexUtils.checkBirthday("1992-09/03"));
    }

    @Test
    void checkURL() {
        assertTrue(RegexUtils.checkURL("http://www.csdn.net:80"));
        assertTrue(RegexUtils.checkURL("https://blog.example.com.tw/path"));
        assertFalse(RegexUtils.checkURL("not a url"));
    }

    @Test
    void checkIpAddress() {
        assertTrue(RegexUtils.checkIpAddress("192.168.1.1"));
        assertTrue(RegexUtils.checkIpAddress("127.0.0.1"));
        assertFalse(RegexUtils.checkIpAddress("abc"));
    }
}
