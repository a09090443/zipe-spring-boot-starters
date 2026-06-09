package com.example.job;

import com.zipe.quartz.job.QuartzJobFactory;
import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

/**
 * 以 Properties 檔設定的 Quartz 排程範例。
 *
 * <p>透過 {@code quartz-jobs.properties} 靜態宣告排程，
 * 應用程式啟動時由 job-starter 自動讀取並向 Quartz Scheduler 註冊。
 * 每次觸發時於日誌輸出執行類別名稱與當下時間，
 * 可作為驗證排程是否正常執行的基準範例。
 */
@Slf4j
public class ExampleXmlJob extends QuartzJobFactory {

    /**
     * 排程觸發時的實際執行邏輯。
     *
     * <p>由父類別 {@link QuartzJobFactory} 的 {@code execute} 方法呼叫，
     * 並傳入 Quartz 提供的執行上下文。
     * 此處僅記錄 INFO 等級日誌，包含執行類別與當前時間，
     * 方便開發人員確認排程有如期觸發。
     *
     * @param jobExecutionContext Quartz 排程執行上下文，包含觸發器、排程器等執行環境資訊
     */
    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) {
        log.info("{}執行, 當前的時間:{}", this.getClass(), DateTimeUtils.getDateNow());
    }
}
