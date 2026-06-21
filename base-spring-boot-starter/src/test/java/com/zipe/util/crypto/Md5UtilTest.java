package com.zipe.util.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 驗證 Md5Util 雜湊輸出正確性（僅供非安全用途；MD5 已標記為 Deprecated）。
 */
@SuppressWarnings("deprecation")
class Md5UtilTest {

    /**
     * "abc" 的 32 位小寫 MD5 為已知固定值。
     */
    @Test
    void parseStrToMd5L32ProducesKnownDigest() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Md5Util.parseStrToMd5L32("abc"));
    }

    /**
     * 32 位大寫 MD5 應為小寫版本之大寫。
     */
    @Test
    void parseStrToMd5U32ProducesUpperCase() {
        assertEquals("900150983CD24FB0D6963F7D28E17F72", Md5Util.parseStrToMd5U32("abc"));
    }
}
