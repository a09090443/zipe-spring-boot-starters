package com.zipe.quartz.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 固化 {@link ScheduleJobStatusEnum} 各常數的整數狀態碼，避免維護時意外改動。
 */
class ScheduleJobStatusEnumTest {

    @Test
    void statusCodesMatchContract() {
        assertEquals(0, ScheduleJobStatusEnum.DELETE.status());
        assertEquals(1, ScheduleJobStatusEnum.START.status());
        assertEquals(2, ScheduleJobStatusEnum.PAUSE.status());
        assertEquals(3, ScheduleJobStatusEnum.RESUME.status());
        assertEquals(4, ScheduleJobStatusEnum.MERGE.status());
        assertEquals(5, ScheduleJobStatusEnum.RUNNING.status());
        assertEquals(6, ScheduleJobStatusEnum.ERROR.status());
        assertEquals(7, ScheduleJobStatusEnum.COMPLETE.status());
        assertEquals(8, ScheduleJobStatusEnum.ONCE.status());
    }
}
