package com.zipe.quartz.enums;

import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CronScheduleBuilder;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;

/**
 * 排程類型列舉，定義 Quartz 排程器支援的各種觸發週期。
 *
 * <p>每個列舉值對應一種時間單位，並各自實作 {@link #setCycle(int)} 或
 * {@link #setExpression(String)} 方法，回傳對應的 Quartz {@link ScheduleBuilder}。</p>
 *
 * <ul>
 *   <li>{@link #NOW} — 立即執行一次（不重複）</li>
 *   <li>{@link #SECOND} / {@link #MINUTE} / {@link #HOUR} — 固定秒/分/時間隔重複執行</li>
 *   <li>{@link #DAY} / {@link #WEEK} / {@link #MONTH} / {@link #YEAR} — 日曆間隔重複執行</li>
 *   <li>{@link #CRON} — 依 Cron 表達式執行</li>
 * </ul>
 */
public enum ScheduleEnum {
    /**
     * 立即執行一次，不重複。
     * 若排程錯過觸發時間，採用「立即補發」（FireNow）策略。
     */
    NOW(0) {
        @Override
        public SimpleScheduleBuilder setCycle(int interval) {
            return SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow();
        }
    },
    /**
     * 以秒為單位，按指定間隔重複執行。
     */
    SECOND(1) {
        @Override
        public ScheduleBuilder setCycle(int interval) {
            return SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever();
        }
    },
    /**
     * 以分鐘為單位，按指定間隔重複執行。
     */
    MINUTE(2) {
        @Override
        public ScheduleBuilder setCycle(int interval) {
            return SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(interval).repeatForever();
        }
    },
    /**
     * 以小時為單位，按指定間隔重複執行。
     */
    HOUR(3) {
        @Override
        public ScheduleBuilder setCycle(int interval) {
            return SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(interval).repeatForever();
        }
    },
    /**
     * 以天為單位，按指定間隔重複執行（日曆間隔，可正確處理夏令時間）。
     */
    DAY(4) {
        @Override
        public ScheduleBuilder setCycle(int interval) {
            return CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInDays(interval);
        }
    },
    /**
     * 以週為單位，按指定間隔重複執行（日曆間隔）。
     */
    WEEK(5) {
        @Override
        public ScheduleBuilder setCycle(int interval) {
            return CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(interval);
        }
    },
    /**
     * 以月為單位，按指定間隔重複執行（日曆間隔）。
     */
    MONTH(6) {
        @Override
        public ScheduleBuilder setCycle(int interval) {
            return CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInMonths(interval);
        }
    },
    /**
     * 以年為單位，按指定間隔重複執行（日曆間隔）。
     */
    YEAR(7) {
        @Override
        public ScheduleBuilder setCycle(int interval) {
            return CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInYears(interval);
        }
    },
    /**
     * 依 Cron 表達式執行，提供最靈活的排程設定方式。
     * 此列舉值不使用 timeUnit，因此採用無參數建構子。
     */
    CRON {
        @Override
        public ScheduleBuilder setExpression(String timeExpression) {
            return CronScheduleBuilder.cronSchedule(timeExpression);
        }
    };

    /**
     * 依固定時間間隔建立 {@link ScheduleBuilder}。
     * 預設實作為立即觸發（FireNow），各列舉值可覆寫以提供特定行為。
     *
     * @param interval 觸發間隔數值（單位由各列舉值決定）
     * @return 對應的 Quartz {@link ScheduleBuilder}
     */
	public ScheduleBuilder setCycle(int interval){
		return SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow();
	};

    /**
     * 依 Cron 表達式建立 {@link ScheduleBuilder}。
     * 預設實作為立即觸發（FireNow），僅 {@link #CRON} 列舉值會覆寫此方法。
     *
     * @param timeExpression Cron 表達式字串（例如 {@code "0 0 12 * * ?"}）
     * @return 對應的 Quartz {@link ScheduleBuilder}
     */
	public ScheduleBuilder setExpression(String timeExpression){
		return SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow();
	};

    /** 時間單位的數值代碼，對應資料庫或設定檔中的排程類型識別碼。 */
    public int timeUnit;

    /**
     * 取得此排程類型對應的時間單位代碼。
     *
     * @return 時間單位代碼
     */
    public int getTimeUnit() {
        return timeUnit;
    }

    /** 無參數建構子，供 {@link #CRON} 使用（不需要時間單位代碼）。 */
    ScheduleEnum() {
    }

    /**
     * 帶時間單位代碼的建構子。
     *
     * @param timeUnit 時間單位的數值代碼
     */
    ScheduleEnum(int timeUnit) {
        this.timeUnit = timeUnit;
    }

    /**
     * 依時間單位代碼查找對應的 {@link ScheduleEnum} 列舉值。
     *
     * @param timeUnit 時間單位的數值代碼
     * @return 對應的 {@link ScheduleEnum}；若找不到則回傳 {@code null}
     */
    public static ScheduleEnum getTimeUnit(int timeUnit) {
        for (ScheduleEnum scheduleEnum : ScheduleEnum.values()) {
            if (scheduleEnum.getTimeUnit() == timeUnit) {
                return scheduleEnum;
            }
        }
        return null;
    }
}
