package com.zipe.quartz.enums;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CronScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;

/**
 * 驗證 {@link ScheduleEnum} 各列舉值回傳正確的 ScheduleBuilder 子型別與時間單位查找。
 */
class ScheduleEnumTest {

    /**
     * 秒/分/時類型應回傳 SimpleScheduleBuilder。
     */
    @Test
    void simpleIntervalTypesReturnSimpleScheduleBuilder() {
        assertInstanceOf(SimpleScheduleBuilder.class, ScheduleEnum.NOW.setCycle(0));
        assertInstanceOf(SimpleScheduleBuilder.class, ScheduleEnum.SECOND.setCycle(5));
        assertInstanceOf(SimpleScheduleBuilder.class, ScheduleEnum.MINUTE.setCycle(5));
        assertInstanceOf(SimpleScheduleBuilder.class, ScheduleEnum.HOUR.setCycle(1));
    }

    /**
     * 日/週/月/年類型應回傳 CalendarIntervalScheduleBuilder。
     */
    @Test
    void calendarIntervalTypesReturnCalendarScheduleBuilder() {
        assertInstanceOf(CalendarIntervalScheduleBuilder.class, ScheduleEnum.DAY.setCycle(1));
        assertInstanceOf(CalendarIntervalScheduleBuilder.class, ScheduleEnum.WEEK.setCycle(1));
        assertInstanceOf(CalendarIntervalScheduleBuilder.class, ScheduleEnum.MONTH.setCycle(1));
        assertInstanceOf(CalendarIntervalScheduleBuilder.class, ScheduleEnum.YEAR.setCycle(1));
    }

    /**
     * CRON 類型的 setExpression 應回傳 CronScheduleBuilder。
     */
    @Test
    void cronReturnsCronScheduleBuilder() {
        assertInstanceOf(CronScheduleBuilder.class, ScheduleEnum.CRON.setExpression("0 0 12 * * ?"));
    }

    /**
     * getTimeUnit 應依代碼查找對應列舉；未知代碼回傳 null。
     */
    @Test
    void getTimeUnitLooksUpByCode() {
        assertSame(ScheduleEnum.SECOND, ScheduleEnum.getTimeUnit(1));
        assertSame(ScheduleEnum.YEAR, ScheduleEnum.getTimeUnit(7));
        assertNull(ScheduleEnum.getTimeUnit(99));
    }
}
