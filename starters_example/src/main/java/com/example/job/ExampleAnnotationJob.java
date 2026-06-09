package com.example.job;

import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 使用 Spring {@code @Scheduled} 注解方式定義的範例排程任務。
 *
 * <p>示範如何透過在方法上標注 {@code @Scheduled(cron = "...")} 來
 * 設定固定 Cron 表達式排程，無需依賴資料庫或外部設定檔。
 * 適合排程週期固定、不需要動態調整的情境。
 */
@Slf4j
@Component
public class ExampleAnnotationJob {

    /**
     * 每 20 秒執行一次的範例排程方法。
     *
     * <p>記錄目前類別名稱與執行時間，用於驗證排程是否正常觸發。
     * Cron 表達式 {@code "0/20 * * * * ?"} 表示從第 0 秒起，每隔 20 秒觸發一次。
     */
    @Scheduled(cron = "0/20 * * * * ?")
    public void example() {
        log.info("{}執行, 當前的時間:{}", this.getClass(), DateTimeUtils.getDateNow());
    }
}
