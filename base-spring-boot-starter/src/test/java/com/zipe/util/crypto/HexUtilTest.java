package com.zipe.util.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link HexUtil} 的位元組與十六進位字串互轉，以及空白判斷邏輯。
 */
class HexUtilTest {

    /**
     * byte2hex 應輸出大寫且每個 byte 補滿兩位（含個位數補零）。
     */
    @Test
    void byte2hexProducesUpperCasePaddedString() {
        assertEquals("0AFF", HexUtil.byte2hex(new byte[] {0x0A, (byte) 0xFF}));
        assertEquals("00", HexUtil.byte2hex(new byte[] {0x00}));
    }

    /**
     * hex2byte 應為 byte2hex 的反向操作，往返後位元組陣列相同。
     */
    @Test
    void hex2byteIsInverseOfByte2hex() {
        byte[] origin = new byte[] {0x01, 0x23, (byte) 0xAB, (byte) 0xFF};
        assertArrayEquals(origin, HexUtil.hex2byte(HexUtil.byte2hex(origin)));
    }

    /**
     * hex2byte 對 null 與空白輸入應回傳 null。
     */
    @Test
    void hex2byteReturnsNullForBlankInput() {
        assertNull(HexUtil.hex2byte(null));
        assertNull(HexUtil.hex2byte(""));
        assertNull(HexUtil.hex2byte("   "));
    }

    /**
     * isBlank 對 null、空字串、全空白回傳 true；對非空字串回傳 false。
     */
    @Test
    void isBlankDistinguishesBlankFromNonBlank() {
        assertTrue(HexUtil.isBlank(null));
        assertTrue(HexUtil.isBlank(""));
        assertTrue(HexUtil.isBlank("  \t "));
        assertFalse(HexUtil.isBlank("a"));
    }
}
