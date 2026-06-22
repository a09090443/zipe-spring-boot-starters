package com.zipe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link DateFormatter} 對 timestamp 字串與 {@link Date} 的互轉及 null/空字串防禦。
 */
class DateFormatterTest {

    private final DateFormatter formatter = new DateFormatter();

    /**
     * print(null) 應回傳 null。
     */
    @Test
    void printReturnsNullForNullDate() {
        assertNull(formatter.print(null, Locale.getDefault()));
    }

    /**
     * print(Date) 應回傳該日期的 epoch 毫秒字串。
     */
    @Test
    void printReturnsEpochMillis() {
        assertEquals("1000", formatter.print(new Date(1000L), Locale.getDefault()));
    }

    /**
     * parse 對 null 與空字串應回傳 null。
     */
    @Test
    void parseReturnsNullForBlank() {
        assertNull(formatter.parse(null, Locale.getDefault()));
        assertNull(formatter.parse("", Locale.getDefault()));
    }

    /**
     * parse 合法 timestamp 字串應回傳對應時間的 Date。
     */
    @Test
    void parseReturnsDateForValidTimestamp() {
        Date parsed = formatter.parse("1000", Locale.getDefault());
        assertEquals(1000L, parsed.getTime());
    }

    /**
     * parse 非數字字串應拋出 NumberFormatException。
     */
    @Test
    void parseThrowsForNonNumeric() {
        assertThrows(NumberFormatException.class, () -> formatter.parse("not-a-number", Locale.getDefault()));
    }
}
