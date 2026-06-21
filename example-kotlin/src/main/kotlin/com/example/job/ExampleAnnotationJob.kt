package com.example.job

import com.zipe.util.time.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 使用 Spring [Scheduled] 注解方式定義的範例排程任務。
 *
 * 示範如何透過在方法上標注 `@Scheduled(cron = "...")` 來
 * 設定固定 Cron 表達式排程，無需依賴資料庫或外部設定檔。
 * 適合排程週期固定、不需要動態調整的情境。
 */
@Component
class ExampleAnnotationJob {

    private val log = LoggerFactory.getLogger(ExampleAnnotationJob::class.java)

    /**
     * 每 20 秒執行一次的範例排程方法。
     *
     * 記錄目前類別名稱與執行時間，用於驗證排程是否正常觸發。
     * Cron 表達式 `"0/20 * * * * ?"` 表示從第 0 秒起，每隔 20 秒觸發一次。
     */
    @Scheduled(cron = "0/20 * * * * ?")
    fun example() {
        log.info("{}執行, 當前的時間:{}", this.javaClass, DateTimeUtils.getDateNow())
    }
}
