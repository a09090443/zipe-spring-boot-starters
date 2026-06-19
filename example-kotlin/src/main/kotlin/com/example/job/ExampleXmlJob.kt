package com.example.job

import com.zipe.quartz.job.QuartzJobFactory
import com.zipe.util.time.DateTimeUtils
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

/**
 * 以 Properties 檔設定的 Quartz 排程範例。
 *
 * 透過 `quartz-jobs.properties` 靜態宣告排程，
 * 應用程式啟動時由 job-starter 自動讀取並向 Quartz Scheduler 註冊。
 * 每次觸發時於日誌輸出執行類別名稱與當下時間，
 * 可作為驗證排程是否正常執行的基準範例。
 */
class ExampleXmlJob : QuartzJobFactory() {

    private val log = LoggerFactory.getLogger(ExampleXmlJob::class.java)

    /**
     * 排程觸發時的實際執行邏輯。
     *
     * 由父類別 [QuartzJobFactory] 的 `execute` 方法呼叫，
     * 並傳入 Quartz 提供的執行上下文。
     * 此處僅記錄 INFO 等級日誌，包含執行類別與當前時間，
     * 方便開發人員確認排程有如期觸發。
     *
     * @param jobExecutionContext Quartz 排程執行上下文，包含觸發器、排程器等執行環境資訊
     */
    override fun executeJob(jobExecutionContext: JobExecutionContext) {
        log.info("{}執行, 當前的時間:{}", this.javaClass, DateTimeUtils.getDateNow())
    }
}
