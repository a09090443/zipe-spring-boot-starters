package com.zipe.quartz.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 測試用排程任務，繼承 {@link QuartzJobFactory} 並實作 {@code executeJob} 方法。
 * <p>
 * 執行時會將當前時間以 {@code yyyy-MM-dd HH:mm:ss} 格式輸出至 INFO 日誌，
 * 可作為驗證 Quartz 排程是否正常觸發的最小範例。
 * </p>
 *
 * @author : Gary Tsai
 **/
@Slf4j
public class TestJob extends QuartzJobFactory {

    /**
     * 排程觸發時執行的業務邏輯。
     * <p>
     * 取得系統當前時間並格式化後寫入日誌，
     * 供開發階段確認排程是否依設定的 Cron 運行。
     * </p>
     *
     * @param jobExecutionContext Quartz 提供的執行上下文，包含觸發器與排程相關資訊
     * @throws Exception 執行過程中發生的任何例外
     */
    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) throws Exception {
        // 取得當前時間並格式化為易讀字串，方便日誌追蹤
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        log.info("TestJob執行, 當前的時間: " + now);
    }
}
