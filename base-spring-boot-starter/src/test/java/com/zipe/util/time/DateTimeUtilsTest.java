package com.zipe.util.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link DateTimeUtils} 中與「當前時間」無關的純轉換與計算邏輯。
 * <p>避免測試依賴系統當下時間的方法，僅針對輸入決定輸出的方法。</p>
 */
class DateTimeUtilsTest {

    /**
     * 西元年月 yyyyMM 轉民國年月 yyyMM。
     */
    @Test
    void transferADDateToMinguoDate() {
        assertEquals("11301", DateTimeUtils.transferADDateToMinguoDate("202401"));
    }

    /**
     * 民國年 yyyMMdd 轉西元年 yyyy-MM-dd。
     */
    @Test
    void transferMinguoDateToADDate() {
        assertEquals("2024-01-01", DateTimeUtils.transferMinguoDateToADDate("1130101"));
    }

    /**
     * firstMinDateOfSingleMonth：偶數月往前推一個月取得單（奇）月第一天 00:00。
     */
    @Test
    void firstMinDateOfSingleMonthForEvenMonth() {
        LocalDateTime result = DateTimeUtils.firstMinDateOfSingleMonth(LocalDateTime.of(2024, 4, 15, 10, 30));
        assertEquals(3, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
        assertEquals(LocalTime.MIN, result.toLocalTime());
    }

    /**
     * firstMinDateOfSingleMonth：奇數月直接取得該月第一天 00:00。
     */
    @Test
    void firstMinDateOfSingleMonthForOddMonth() {
        LocalDateTime result = DateTimeUtils.firstMinDateOfSingleMonth(LocalDateTime.of(2024, 5, 15, 10, 30));
        assertEquals(5, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
    }

    /**
     * getUntilDay / getUntilMonth 計算兩日期間隔。
     */
    @Test
    void getUntilDayAndMonth() {
        Date d1 = DateTimeUtils.localDateToDate(LocalDate.of(2024, 1, 1));
        Date dDay = DateTimeUtils.localDateToDate(LocalDate.of(2024, 1, 11));
        Date dMonth = DateTimeUtils.localDateToDate(LocalDate.of(2024, 3, 1));
        assertEquals(10L, DateTimeUtils.getUntilDay(d1, dDay));
        assertEquals(2L, DateTimeUtils.getUntilMonth(d1, dMonth));
    }

    /**
     * dateToLocalDate 與 localDateToDate 互為往返。
     */
    @Test
    void dateToLocalDateRoundTrip() {
        LocalDate origin = LocalDate.of(2024, 6, 15);
        assertEquals(origin, DateTimeUtils.dateToLocalDate(DateTimeUtils.localDateToDate(origin)));
    }

    /**
     * DateToJava8Date 依型別代碼回傳對應型別；未知代碼回傳 null。
     */
    @Test
    void dateToJava8DateMapsByType() {
        Date now = new Date();
        assertInstanceOf(LocalDateTime.class, DateTimeUtils.DateToJava8Date(now, 1));
        assertInstanceOf(LocalDate.class, DateTimeUtils.DateToJava8Date(now, 2));
        assertInstanceOf(LocalTime.class, DateTimeUtils.DateToJava8Date(now, 3));
        assertNull(DateTimeUtils.DateToJava8Date(now, 9));
    }
}
