package com.zipe.quartz.enums;

import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CronScheduleBuilder;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;

public enum ScheduleEnum {
    NOW(0) {
        @Override
        public SimpleScheduleBuilder setCycle(int interval, int repeatCount) {
            return SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow();
        }
    },
    SECOND(1) {
        @Override
        public ScheduleBuilder setCycle(int interval, int repeatCount) {
            return SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever();
        }
    },
    MINUTE(2) {
        @Override
        public ScheduleBuilder setCycle(int interval, int repeatCount) {
            return SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(interval).repeatForever();
        }
    },
    HOUR(3) {
        @Override
        public ScheduleBuilder setCycle(int interval, int repeatCount) {
            return SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(interval).repeatForever();
        }
    },
    DAY(4) {
        @Override
        public ScheduleBuilder setCycle(int interval, int repeatCount) {
            return CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInDays(interval);
        }
    },
    WEEK(5) {
        @Override
        public ScheduleBuilder setCycle(int interval, int repeatCount) {
            return CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(interval);
        }
    },
    MONTH(6) {
        @Override
        public ScheduleBuilder setCycle(int interval, int repeatCount) {
            return CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInMonths(interval);
        }
    },
    YEAR(7) {
        @Override
        public ScheduleBuilder setCycle(int interval, int repeatCount) {
            return CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInYears(interval);
        }
    },
    CRON {
        @Override
        public ScheduleBuilder setExpression(String timeExpression) {
            return CronScheduleBuilder.cronSchedule(timeExpression);
        }
    };

	public ScheduleBuilder setCycle(int interval, int repeatCount){
		return SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow();
	};

	public ScheduleBuilder setExpression(String timeExpression){
		return SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow();
	};

    public int timeUnit;

    public int getTimeUnit() {
        return timeUnit;
    }

    ScheduleEnum() {
    }

    ScheduleEnum(int timeUnit) {
        this.timeUnit = timeUnit;
    }

    public static ScheduleEnum getTimeUnit(int timeUnit) {
        for (ScheduleEnum scheduleEnum : ScheduleEnum.values()) {
            if (scheduleEnum.getTimeUnit() == timeUnit) {
                return scheduleEnum;
            }
        }
        return null;
    }
}
