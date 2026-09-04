package com.zipe.util.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link Sha256Util} 雜湊輸出正確性。
 */
class Sha256UtilTest {

    /**
     * 空字串的 SHA-256 為公開標準測試向量固定值。
     */
    @Test
    void sha256HexOfEmptyStringProducesKnownDigest() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Sha256Util.sha256Hex(""));
    }

    /**
     * "abc" 的 SHA-256 為公開標準測試向量固定值。
     */
    @Test
    void sha256HexOfAbcProducesKnownDigest() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Sha256Util.sha256Hex("abc"));
    }

    /**
     * 輸出長度固定為 64 個十六進位字元。
     */
    @Test
    void sha256HexOutputLengthIsAlways64() {
        assertEquals(64, Sha256Util.sha256Hex("任意長度的字串").length());
        assertEquals(64, Sha256Util.sha256Hex("a").length());
    }

    /**
     * null 輸入應丟出 IllegalArgumentException，而非沿用 Md5Util 隱性拋出 NPE 的行為。
     */
    @Test
    void sha256HexThrowsForNullInput() {
        assertThrows(IllegalArgumentException.class, () -> Sha256Util.sha256Hex(null));
    }
}
