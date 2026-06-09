package com.zipe.util.time;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * 日期時間工具類別，提供常用的日期時間格式化、轉換、計算與民國年處理等靜態方法。
 * <p>
 * 所有取得「當前時間」的方法預設以 UTC+8（台灣時區）為基準。
 * 本類別為純靜態工具類別，不需實例化。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/9 上午 10:23
 **/
public class DateTimeUtils {
    /** UTC+8 時區偏移字串，對應台灣標準時間（CST） */
    public static final String UTC_ADD_8 = "+8";

    /** 格式：yyyy-MM-dd HH:mm:ss（例：2024-01-15 10:30:00） */
    public static DateTimeFormatter dateTimeFormate1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 格式：yyyy-MM-dd（例：2024-01-15） */
    public static DateTimeFormatter dateTimeFormate2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 格式：HH:mm:ss（例：10:30:00） */
    public static DateTimeFormatter dateTimeFormate3 = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** 格式：yyyy/MM/dd HH:mm:ss（例：2024/01/15 10:30:00） */
    public static DateTimeFormatter dateTimeFormate4 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    /** 格式：yyyyMMdd HH:mm:ss（例：20240115 10:30:00） */
    public static DateTimeFormatter dateTimeFormate5 = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    /** 格式：yyyy/MM/dd（例：2024/01/15） */
    public static DateTimeFormatter dateTimeFormate6 = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    /** 格式：yyyyMMdd（西元年，例：20240115） */
    public static DateTimeFormatter dateTimeFormate7 = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 格式：yyyMMdd（民國年，例：1130115） */
    public static DateTimeFormatter dateTimeFormate8 = DateTimeFormatter.ofPattern("yyyMMdd");
    /** 格式：yyyy（西元年份，例：2024） */
    public static DateTimeFormatter dateTimeFormate9 = DateTimeFormatter.ofPattern("yyyy");
    /** 格式：MM（月份，例：01） */
    public static DateTimeFormatter dateTimeFormate10 = DateTimeFormatter.ofPattern("MM");
    /** 格式：dd（日期，例：15） */
    public static DateTimeFormatter dateTimeFormate11 = DateTimeFormatter.ofPattern("dd");
    /** 格式：yyyyMM（西元年月，例：202401） */
    public static DateTimeFormatter dateTimeFormate12 = DateTimeFormatter.ofPattern("yyyyMM");
    /** 格式：yyyMM（民國年月，例：11301） */
    public static DateTimeFormatter dateTimeFormate13 = DateTimeFormatter.ofPattern("yyyMM");
    /** 格式：yyy/MM（民國年/月，例：113/01） */
    public static DateTimeFormatter dateTimeFormate14 = DateTimeFormatter.ofPattern("yyy/MM");
    /** 格式：dd/MM/yy（例：15/01/24） */
    public static DateTimeFormatter dateTimeFormate15 = DateTimeFormatter.ofPattern("dd/MM/yy");
    /** 格式：HHmmss（時間無分隔符，例：103000） */
    public static DateTimeFormatter dateTimeFormate16 = DateTimeFormatter.ofPattern("HHmmss");
    /** 格式：yyy（民國年份，例：113） */
    public static DateTimeFormatter dateTimeFormate17 = DateTimeFormatter.ofPattern("yyy");
    /** 格式：yyyyMMddHHmmss（完整日期時間無分隔符，例：20240115103000） */
    public static DateTimeFormatter dateTimeFormate18 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 取得 UTC+8 時區的當前日期時間，並以指定格式回傳字串。
     *
     * @param formatter 日期時間格式器
     * @return 格式化後的當前日期時間字串
     */
    public static String getDateNow(DateTimeFormatter formatter) {
        return getDateNow().format(formatter);
    }

    /**
     * 取得 UTC+8 時區的當前日期時間。
     *
     * @return 當前的 {@link LocalDateTime}（UTC+8）
     */
    public static LocalDateTime getDateNow() {
        return LocalDateTime.now(ZoneOffset.of(UTC_ADD_8));
    }

    /**
     * 取得當前年份加減指定年數後的日期字串。
     *
     * @param yearsToAddOrSubtract 後幾年傳正整數，前幾年傳負數
     * @param formatter            日期格式器；為 {@code null} 時以 {@link LocalDate#toString()} 格式回傳
     * @return 計算後的日期字串
     */
    public static String getMinusOrPlusYears(long yearsToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusYears(yearsToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusYears(yearsToAddOrSubtract) + "";
        return date;
    }

    /**
     * 取得當前月份加減指定月數後的日期字串。
     *
     * @param monthsToAddOrSubtract 後幾月傳正整數，前幾月傳負數
     * @param formatter             日期格式器；為 {@code null} 時以 {@link LocalDate#toString()} 格式回傳
     * @return 計算後的日期字串
     */
    public static String getMinusOrPlusMonths(long monthsToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusMonths(monthsToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusMonths(monthsToAddOrSubtract) + "";
        return date;
    }

    /**
     * 以指定起始日期為基準，取得加減指定月數後的日期字串。
     *
     * @param monthsToAddOrSubtract 後幾月傳正整數，前幾月傳負數
     * @param formatter             日期格式器；為 {@code null} 時以 {@link LocalDate#toString()} 格式回傳
     * @param startDate             需計算的起始日期
     * @return 計算後的日期字串
     */
    public static String getMinusOrPlusMonthsByStartDate(long monthsToAddOrSubtract, DateTimeFormatter formatter, LocalDateTime startDate) {
        String date = !Objects.isNull(formatter) ? startDate.plusMonths(monthsToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusMonths(monthsToAddOrSubtract) + "";
        return date;
    }

    /**
     * 取得當前日期加減指定天數後的日期字串。
     *
     * @param daysToAddOrSubtract 後幾日傳正整數，前幾日傳負數
     * @param formatter           日期格式器；為 {@code null} 時以 {@link LocalDate#toString()} 格式回傳
     * @return 計算後的日期字串
     */
    public static String getMinusOrPlusDays(long daysToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusDays(daysToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusDays(daysToAddOrSubtract) + "";
        return date;
    }

    /**
     * 取得當前日期加減指定週數後的日期字串。
     *
     * @param weeksToAddOrSubtract 後幾星期傳正整數，前幾星期傳負數
     * @param formatter            日期格式器；為 {@code null} 時以 {@link LocalDate#toString()} 格式回傳
     * @return 計算後的日期字串
     */
    public static String getMinusOrPlusWeeks(long weeksToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusWeeks(weeksToAddOrSubtract).format(formatter) :
                LocalDate.now(ZoneOffset.of(UTC_ADD_8)).plusWeeks(weeksToAddOrSubtract) + "";
        return date;
    }

    /**
     * 取得當前時間加減指定小時數後的時間字串。
     *
     * @param hoursToAddOrSubtract 後幾小時傳正整數，前幾小時傳負數
     * @param formatter            日期時間格式器；不得為 {@code null}
     * @return 計算後的時間字串
     */
    public static String getMinusOrPlusHours(long hoursToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusHours(hoursToAddOrSubtract).format(formatter) :
                LocalTime.now(ZoneOffset.of(UTC_ADD_8)).plusHours(hoursToAddOrSubtract).format(formatter);
        return date;
    }

    /**
     * 取得當前分鐘加減指定分鐘數後的時間字串。
     *
     * @param minutesToAddOrSubtract 後幾分鐘傳正整數，前幾分鐘傳負數
     * @param formatter              日期時間格式器；不得為 {@code null}
     * @return 計算後的時間字串
     */
    public static String getMinusOrPlusMinutes(long minutesToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusMinutes(minutesToAddOrSubtract).format(formatter) :
                LocalTime.now(ZoneOffset.of(UTC_ADD_8)).plusMinutes(minutesToAddOrSubtract).format(formatter);
        return date;
    }

    /**
     * 取得當前秒加減指定秒數後的時間字串。
     *
     * @param secondsToAddOrSubtract 後幾秒傳正整數，前幾秒傳負數
     * @param formatter              日期時間格式器；不得為 {@code null}
     * @return 計算後的時間字串
     */
    public static String getMinusOrPlusSeconds(long secondsToAddOrSubtract, DateTimeFormatter formatter) {
        String date = !Objects.isNull(formatter) ? getDateNow().plusSeconds(secondsToAddOrSubtract).format(formatter) :
                LocalTime.now(ZoneOffset.of(UTC_ADD_8)).plusSeconds(secondsToAddOrSubtract).format(formatter);
        return date;
    }

    /**
     * 將 {@link Date} 轉換為 Java 8 日期時間型別。
     *
     * @param date 要轉換的 {@link Date} 物件
     * @param type 目標型別代碼：1 為 {@link LocalDateTime}；2 為 {@link LocalDate}；3 為 {@link LocalTime}
     * @return 對應的 Java 8 日期時間物件；若 {@code type} 不符合任何 case 則回傳 {@code null}
     */
    public static Object DateToJava8Date(Date date, Integer type) {
        Instant instant = date.toInstant();
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zone);
        Object java8Date = null;
        switch (type) {
            case 1:
                java8Date = dateTime;
                break;
            case 2:
                java8Date = dateTime.toLocalDate();
                break;
            case 3:
                java8Date = dateTime.toLocalTime();
                break;
        }
        return java8Date;
    }

    /**
     * 將 Java 8 日期時間型別（{@link LocalDateTime}、{@link LocalDate}、{@link LocalTime}）轉換為 {@link Date}。
     * <p>
     * 若傳入 {@link LocalTime}，日期部分以 UTC+8 當日為準。
     * </p>
     *
     * @param java8Date {@link LocalDateTime}、{@link LocalDate} 或 {@link LocalTime} 物件
     * @return 對應的 {@link Date}；若無法解析型別則回傳 {@code null}
     */
    public static Date Java8DateToDate(Object java8Date) {
        ZoneId zone = ZoneId.of(UTC_ADD_8);
        Instant instant = null;
        Date date = null;
        if (java8Date instanceof LocalDateTime) {
            instant = ((LocalDateTime) java8Date).atZone(zone).toInstant();
        }
        if (java8Date instanceof LocalDate) {
            instant = ((LocalDate) java8Date).atStartOfDay().atZone(zone).toInstant();
        }
        if (java8Date instanceof LocalTime) {
            LocalDateTime localDateTime = LocalDateTime.of(LocalDate.now(ZoneOffset.of(UTC_ADD_8)), LocalTime.now(ZoneOffset.of(UTC_ADD_8)));
            instant = localDateTime.atZone(zone).toInstant();
        }
        return instant == null ? null : Date.from(instant);
    }

    /**
     * 將日期時間字串依指定格式解析為 {@link LocalDateTime}。
     *
     * @param date              日期時間字串
     * @param dateTimeFormatter 對應的格式器
     * @return 解析後的 {@link LocalDateTime}
     */
    public static LocalDateTime getDateTime(String date, DateTimeFormatter dateTimeFormatter) {
        return LocalDateTime.parse(date, dateTimeFormatter);
    }

    /**
     * 將日期字串依指定格式解析為 {@link LocalDate}。
     *
     * @param date              日期字串
     * @param dateTimeFormatter 對應的格式器
     * @return 解析後的 {@link LocalDate}
     */
    public static LocalDate getDate(String date, DateTimeFormatter dateTimeFormatter) {
        return LocalDate.parse(date, dateTimeFormatter);
    }

    /**
     * 將 {@link Date} 轉換為 UTC+8 時區的 {@link LocalDateTime}。
     *
     * @param date 要轉換的 {@link Date} 物件
     * @return 對應的 {@link LocalDateTime}（UTC+8）
     */
    public static LocalDateTime dateToDateTime(Date date) {
        Instant instant = date.toInstant();
        return LocalDateTime.ofInstant(instant, ZoneId.of(UTC_ADD_8));
    }

    /**
     * 將 {@link Date} 轉換為 UTC+8 時區的 {@link LocalDate}。
     *
     * @param date 要轉換的 {@link Date} 物件
     * @return 對應的 {@link LocalDate}（UTC+8）
     */
    public static LocalDate dateToLocalDate(Date date) {
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.of(UTC_ADD_8)).toLocalDate();
    }

    /**
     * 將 {@link LocalDate} 轉換為 {@link Date}，時間部分為該日 00:00:00（UTC+8）。
     *
     * @param localDate 要轉換的 {@link LocalDate} 物件
     * @return 對應的 {@link Date}
     */
    public static Date localDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    /**
     * 將 {@link Date} 轉換為 UTC+8 時區的 {@link LocalTime}（僅保留時間部分）。
     *
     * @param date 要轉換的 {@link Date} 物件
     * @return 對應的 {@link LocalTime}（UTC+8）
     */
    public static LocalTime dateToLocalTime(Date date) {
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.of(UTC_ADD_8)).toLocalTime();
    }

    /**
     * 取得指定日期加上若干天後，該日的最小時間（00:00:00）。
     *
     * @param date  基準日期
     * @param after 要往後加的天數
     * @return 計算後日期的 00:00:00 {@link LocalDateTime}
     */
    public static LocalDateTime afterXDateTimeMIN(Date date, int after) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.plusDays(after);
        localDateTime = localDateTime.with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 取得指定日期加上若干天後，該日的最大時間（23:59:59.999999999）。
     *
     * @param date  基準日期
     * @param after 要往後加的天數
     * @return 計算後日期的 23:59:59.999... {@link LocalDateTime}
     */
    public static LocalDateTime afterXDateTimeMAX(Date date, int after) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.plusDays(after);
        localDateTime = localDateTime.with(LocalTime.MAX);
        return localDateTime;
    }

    /**
     * 取得指定日期減去若干天後，該日的最小時間（00:00:00），並回傳 {@link Date}。
     *
     * @param date   基準日期
     * @param before 要往前減的天數
     * @return 計算後日期的 00:00:00 {@link Date}（UTC+8）
     */
    public static Date beforeXDateTimeMIN(Date date, int before) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.minusDays(before);
        localDateTime = localDateTime.with(LocalTime.MIN);
        return Date.from(localDateTime.atZone(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    /**
     * 取得指定日期減去若干天後，該日的最大時間（23:59:59.999...），並回傳 {@link Date}。
     *
     * @param date   基準日期
     * @param before 要往前減的天數
     * @return 計算後日期的 23:59:59.999... {@link Date}（UTC+8）
     */
    public static Date beforeXDateTimeMAX(Date date, int before) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.minusDays(before);
        localDateTime = localDateTime.with(LocalTime.MAX);
        return Date.from(localDateTime.atZone(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    /**
     * 取得本月第一天的最小時間（00:00:00）。
     *
     * @return 本月 1 日 00:00:00 的 {@link LocalDateTime}
     */
    public static LocalDateTime currentFirstDayOfMonth() {
        LocalDateTime localDateTime = LocalDateTime.now();
        localDateTime = localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 取得指定日期所在月份的最後一天最大時間（23:59:59.999...）。
     *
     * @param date 基準日期
     * @return 該月最後一日 23:59:59.999... 的 {@link LocalDateTime}
     */
    public static LocalDateTime currentXDayOfMonth(Date date) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
        return localDateTime;
    }

    /**
     * 取得指定日期所在月份的第一天最小時間（00:00:00）。
     *
     * @param date 基準日期
     * @return 該月 1 日 00:00:00 的 {@link LocalDateTime}
     */
    public static LocalDateTime beforeXFirstDayOfMonth(Date date) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 以 {@link LocalDateTime} 為基準，往前推指定月數後取得該月第一天 00:00:00。
     *
     * @param localDateTime 基準日期時間
     * @param month         要往前推的月數
     * @return 計算後月份 1 日 00:00:00 的 {@link LocalDateTime}
     */
    public static LocalDateTime preXDayOfMonthMIN(LocalDateTime localDateTime, int month) {
        localDateTime = localDateTime.minusMonths(month);
        localDateTime = localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 以 {@link Date} 為基準，往前推指定月數後取得該月第一天 00:00:00。
     *
     * @param date  基準日期
     * @param month 要往前推的月數
     * @return 計算後月份 1 日 00:00:00 的 {@link LocalDateTime}
     */
    public static LocalDateTime preXDayOfMonthMIN(Date date, int month) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.minusMonths(month);
        localDateTime = localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        return localDateTime;
    }

    /**
     * 以當前時間為基準，往前推指定月數後取得該月最後一天 23:59:59.999...。
     *
     * @param month 要往前推的月數
     * @return 計算後月份最後一日 23:59:59.999... 的 {@link LocalDateTime}
     */
    public static LocalDateTime preXDayOfMonthMAX(int month) {
        LocalDateTime localDateTime = LocalDateTime.now();
        localDateTime = localDateTime.minusMonths(month);
        localDateTime = localDateTime.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
        return localDateTime;
    }

    /**
     * 以指定日期為基準，往前推指定月數後取得該月最後一天 23:59:59.999...。
     *
     * @param date  基準日期
     * @param month 要往前推的月數
     * @return 計算後月份最後一日 23:59:59.999... 的 {@link LocalDateTime}
     */
    public static LocalDateTime preXDayOfMonthMAX(Date date, int month) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.minusMonths(month);
        localDateTime = localDateTime.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
        return localDateTime;
    }

    /**
     * 計算兩個日期之間相差的完整月數（date2 - date1）。
     *
     * @param date1 起始日期
     * @param date2 結束日期
     * @return 相差的月數；date2 早於 date1 時回傳負數
     */
    public static Long getUntilMonth(Date date1, Date date2) {
        LocalDate localDate1 = dateToLocalDate(date1);
        LocalDate localDate2 = dateToLocalDate(date2);
        return ChronoUnit.MONTHS.between(localDate1, localDate2);
    }

    /**
     * 計算兩個日期之間相差的完整天數（date2 - date1）。
     *
     * @param date1 起始日期
     * @param date2 結束日期
     * @return 相差的天數；date2 早於 date1 時回傳負數
     */
    public static Long getUntilDay(Date date1, Date date2) {
        LocalDate localDate1 = dateToLocalDate(date1);
        LocalDate localDate2 = dateToLocalDate(date2);
        return ChronoUnit.DAYS.between(localDate1, localDate2);
    }

    /**
     * 計算兩個日期之間相差的完整小時數（date2 - date1），捨去不足一小時的部分。
     *
     * @param date1 起始日期時間
     * @param date2 結束日期時間
     * @return 相差的小時數（無條件捨去）；date2 早於 date1 時回傳負數
     */
    public static Long getUntilHours(Date date1, Date date2) {
        LocalDateTime localDate1 = dateToDateTime(date1);
        LocalDateTime localDate2 = dateToDateTime(date2);
        Long senonds = Duration.between(localDate1, localDate2).get(ChronoUnit.SECONDS);
        return senonds / 3600;
    }

    /**
     * 計算兩個日期之間相差的小時數，以 {@code double} 回傳（四捨五入至小數點後兩位）。
     *
     * @param date1 起始日期時間
     * @param date2 結束日期時間
     * @return 相差的小時數（含小數，四捨五入至 2 位）；date2 早於 date1 時回傳負數
     */
    public static double getUntilHoursByDouble(Date date1, Date date2) {
        LocalDateTime localDate1 = dateToDateTime(date1);
        LocalDateTime localDate2 = dateToDateTime(date2);
        Long seconds = Duration.between(localDate1, localDate2).get(ChronoUnit.SECONDS);
        BigDecimal secondss = BigDecimal.valueOf(seconds);
        BigDecimal hours = secondss.divide(BigDecimal.valueOf(3600), 2, BigDecimal.ROUND_HALF_UP);
        return hours.doubleValue();
    }

    /**
     * 計算兩個日期之間相差的完整秒數（date2 - date1）。
     *
     * @param date1 起始日期時間
     * @param date2 結束日期時間
     * @return 相差的秒數；date2 早於 date1 時回傳負數
     */
    public static Long getUntilSecond(Date date1, Date date2) {
        LocalDateTime localDate1 = dateToDateTime(date1);
        LocalDateTime localDate2 = dateToDateTime(date2);
        return Duration.between(localDate1, localDate2).get(ChronoUnit.SECONDS);
    }

    /**
     * 取得指定日期當天的最大時間（23:59:59.999...），並回傳 {@link Date}。
     *
     * @param date 基準日期
     * @return 該日 23:59:59.999... 的 {@link Date}（UTC+8）
     */
    public static Date currentMax(Date date) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.with(LocalTime.MAX);
        return Date.from(localDateTime.atZone(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    /**
     * 取得指定日期當天的最小時間（00:00:00），並回傳 {@link Date}。
     *
     * @param date 基準日期
     * @return 該日 00:00:00 的 {@link Date}（UTC+8）
     */
    public static Date currentMin(Date date) {
        LocalDateTime localDateTime = dateToDateTime(date);
        localDateTime = localDateTime.with(LocalTime.MIN);
        return Date.from(localDateTime.atZone(ZoneId.of(UTC_ADD_8)).toInstant());
    }

    /**
     * 取得目前年份的民國年字串（3 位數字，例：113）。
     *
     * @return 目前西元年對應的民國年字串
     */
    public static String getMinguoYear(){
        return MinguoDate.from(LocalDate.now(ZoneOffset.of(UTC_ADD_8))).format(dateTimeFormate17);
    }
    /**
     * Transfer AD date to minguo date.
     * 西元年 yyyyMM 轉 民國年 yyyMM
     *
     * @param dateString the String dateString
     * @return the string
     */
    public static String transferADDateToMinguoDate(String dateString) {
        YearMonth localDate = YearMonth.parse(dateString, dateTimeFormate12);
        return MinguoDate.from(localDate.atDay(1)).format(dateTimeFormate13);
    }

    /**
     * 取得民國年份字串；若目前為一月，自動回傳上一年的民國年，避免報表產生跨年錯誤。
     *
     * @return 民國年字串（3 位數字）；若當前月份為一月則回傳上一年度
     */
    public static String getMinguoYearIfJanuary(){
        if (getDateNow().getMonthValue() == 1) {
            LocalDate localDate = LocalDate.parse(getMinusOrPlusYears(-1, dateTimeFormate7), dateTimeFormate7);
            return MinguoDate.from(localDate).format(dateTimeFormate17);
        } else {
            return getMinguoYear();
        }
    }
    /**
     * Transfer minguo date to AD date.
     * 民國年 yyyMMdd 轉 西元年 yyyy-MM-dd
     *
     * @param dateString the String dateString
     * @return the string
     */
    public static String transferMinguoDateToADDate(String dateString) {
        Chronology chrono = MinguoChronology.INSTANCE;
        DateTimeFormatter df = new DateTimeFormatterBuilder().parseLenient()
                .appendPattern("yyyMMdd")
                .toFormatter()
                .withChronology(chrono)
                .withDecimalStyle(DecimalStyle.of(Locale.getDefault()));
        ChronoLocalDate chDate = chrono.date(df.parse(dateString));
        return LocalDate.from(chDate).format(dateTimeFormate2);
    }

    /**
     * 取得西元年份字串；若目前為一月，自動回傳上一年的西元年，避免跨年統計誤差。
     *
     * @return 西元年份字串（4 位數字）；若當前月份為一月則回傳上一年度
     */
    public static String getLastYearOnJanuary() {
        if (getDateNow().getMonthValue() == 1) {
            return getMinusOrPlusYears(-1, dateTimeFormate9);
        } else {
            return String.valueOf(getDateNow().getYear());
        }
    }

    /**
     * 取得單月（奇數月）的第一天最小時間（00:00:00）。
     * <p>
     * 若傳入為偶數月，自動往前推一個月取得前一個奇數月的第一天 00:00:00；
     * 若傳入為奇數月，直接取得該月第一天 00:00:00。
     * 適用於依單月為單位進行統計的情境。
     * </p>
     *
     * @param localDateTime 基準日期時間
     * @return 對應奇數月 1 日 00:00:00 的 {@link LocalDateTime}
     */
    public static LocalDateTime firstMinDateOfSingleMonth(LocalDateTime localDateTime) {
        int month = localDateTime.getMonthValue();
        if (month % 2 == 0) {
            localDateTime = DateTimeUtils.preXDayOfMonthMIN(localDateTime, 1);
        } else {
            localDateTime = DateTimeUtils.preXDayOfMonthMIN(localDateTime, 0);
        }
        return localDateTime;
    }

    /**
     * 將 {@link LocalTime} 轉換為 {@link Date}，日期部分設為西元元年 1 月 1 日（僅保留時間資訊）。
     * <p>
     * 注意：日期部分不具實際意義，適用於僅需比較時間部分的情境。
     * </p>
     *
     * @param localTime 要轉換的 {@link LocalTime} 物件
     * @return 日期部分為西元元年、僅時間有意義的 {@link Date}
     */
    public static Date localTimeToDate(LocalTime localTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        //assuming year/month/date information is not important
        calendar.set(0, 0, 0, localTime.getHour(), localTime.getMinute(), localTime.getSecond());
        return calendar.getTime();
    }

    /**
     * 將 {@link LocalDateTime} 轉換為 {@link Date}，使用系統預設時區。
     *
     * @param localDateTime 要轉換的 {@link LocalDateTime} 物件
     * @return 對應的 {@link Date}
     */
    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(localDateTime.getYear(), localDateTime.getMonthValue()-1, localDateTime.getDayOfMonth(),
                localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond());
        return calendar.getTime();
    }
    /**
     * 依傳入日期取得前期雙月份的月份字串清單（共兩個月）。
     * <p>
     * 規則：
     * <ul>
     *   <li>傳入偶數月（如 202106）→ 回傳該月前 2、3 個月（如 [03, 04]）</li>
     *   <li>傳入奇數月（如 202105）→ 回傳該月前 1、2 個月（如 [03, 04]）</li>
     * </ul>
     * 例如：傳入 202112 回傳 [09, 10]。
     * </p>
     *
     * @param dateTime 基準日期時間
     * @return 包含兩個月份字串（MM 格式）的清單，索引 0 為較早的月份
     */
    public static ArrayList<String> getPreviousMonths(LocalDateTime dateTime) {
        ArrayList<String> values = new ArrayList<>();
        if (dateTime.getMonthValue() % 2 == 0) {
            values.add(DateTimeUtils.getMinusOrPlusMonthsByStartDate(-2, DateTimeUtils.dateTimeFormate10, dateTime));
            values.add(DateTimeUtils.getMinusOrPlusMonthsByStartDate(-3, DateTimeUtils.dateTimeFormate10, dateTime));
        } else {
            values.add(DateTimeUtils.getMinusOrPlusMonthsByStartDate(-1, DateTimeUtils.dateTimeFormate10, dateTime));
            values.add(DateTimeUtils.getMinusOrPlusMonthsByStartDate(-2, DateTimeUtils.dateTimeFormate10, dateTime));
        }
        return values;
    }

    public static void main(String[] args) {
        System.out.println(DateTimeUtils.getMinusOrPlusMonths(-1, DateTimeUtils.dateTimeFormate14));
    }

}
