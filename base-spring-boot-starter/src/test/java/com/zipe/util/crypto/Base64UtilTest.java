package com.zipe.util.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link Base64Util} 的 Base64 編解碼、空白處理與字元集降級行為。
 */
class Base64UtilTest {

    private Base64Util base64Util;

    @BeforeEach
    void setUp() {
        base64Util = new Base64Util();
    }

    /**
     * 空白輸入應回傳空字串（編碼與解碼皆然）。
     */
    @Test
    void blankInputReturnsEmptyString() {
        assertEquals("", base64Util.getEncrypt(""));
        assertEquals("", base64Util.getEncrypt("  "));
        assertEquals("", base64Util.getDecode(""));
        assertEquals("", base64Util.getDecode(null));
    }

    /**
     * 以預設字元集編碼為已知 Base64 值，且解碼可還原原文。
     */
    @Test
    void encryptThenDecodeRoundTripsWithDefaultCharset() {
        String encoded = base64Util.getEncrypt("Hello");
        assertEquals("SGVsbG8=", encoded);
        assertEquals("Hello", base64Util.getDecode(encoded));
    }

    /**
     * 指定 UTF-8 字元集的編解碼往返應還原原文。
     */
    @Test
    void encryptThenDecodeRoundTripsWithUtf8Charset() {
        String encoded = base64Util.getEncrypt("Hello", "UTF-8");
        assertEquals("Hello", base64Util.getDecode(encoded, "UTF-8"));
    }

    /**
     * 不支援的字元集應降級為系統預設字元集，對 ASCII 內容結果與預設編碼一致。
     */
    @Test
    void unsupportedCharsetFallsBackToDefault() {
        assertEquals(base64Util.getEncrypt("Hello"), base64Util.getEncrypt("Hello", "NO-SUCH-CHARSET"));
    }
}
