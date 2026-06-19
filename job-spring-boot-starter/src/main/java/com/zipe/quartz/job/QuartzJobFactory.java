package com.zipe.quartz.job;

import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quartz 排程任務的抽象基底類別。
 *
 * <p>繼承自 Spring 的 {@link QuartzJobBean}，統一處理排程執行前後的日誌記錄與例外捕捉。
 * 子類別只需實作 {@link #executeJob(JobExecutionContext)} 方法，即可專注於業務邏輯，
 * 無需重複撰寫日誌與錯誤處理樣板程式碼。</p>
 *
 * @author : Gary Tsai
 **/
@Slf4j
public abstract class QuartzJobFactory extends QuartzJobBean {

    /**
     * 排程執行前 Log 紀錄
     *
     * @param jobExecutionContext Quartz 提供的排程執行上下文，包含 JobDetail 與觸發器資訊
     */
    protected void beforeExecuteJobLog(JobExecutionContext jobExecutionContext) {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        log.info("排程名稱:{} 執行開始:時間{}", jobName, DateTimeUtils.getDateNow());
    }
    /**
     * 排程執行後 Log 紀錄
     *
     * @param jobExecutionContext Quartz 提供的排程執行上下文，包含 JobDetail 與觸發器資訊
     */
    protected void afterExecuteJobLog(JobExecutionContext jobExecutionContext) {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        log.info("排程名稱:{} 執行結束:時間{}", jobName, DateTimeUtils.getDateNow());
    }

    /**
     * 排程執行錯誤 Log 紀錄
     *
     * @param jobExecutionContext Quartz 提供的排程執行上下文，包含 JobDetail 與觸發器資訊
     * @param e                  排程執行期間拋出的例外
     */
    protected void errorExecuteJobLog(JobExecutionContext jobExecutionContext, Exception e) {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        log.error("排程名稱:{} 發生錯誤，{}", jobName, e.getMessage());
    }

    /**
     * 排程核心執行入口，由 Quartz 框架呼叫。
     *
     * <p>依序執行：前置日誌記錄 → 呼叫子類別實作的 {@link #executeJob} → 後置日誌記錄。
     * 若 {@link #executeJob} 拋出例外，將以錯誤日誌記錄並繼續完成後置流程，
     * 避免排程框架因未捕捉例外而中斷後續排程。</p>
     *
     * @param jobExecutionContext Quartz 提供的排程執行上下文
     * @throws JobExecutionException 當排程執行框架層級發生不可回復的例外時拋出
     */
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        this.beforeExecuteJobLog(jobExecutionContext);
        try {
            executeJob(jobExecutionContext);
        } catch (Exception e) {
            // 捕捉業務邏輯例外，僅記錄錯誤日誌，不向上拋出，確保後置日誌仍能正常執行
            this.errorExecuteJobLog(jobExecutionContext, e);
        }
        this.afterExecuteJobLog(jobExecutionContext);
    }

    /**
     * 排程預設定義的執行方法，由子類別實作業務邏輯。
     *
     * @param jobExecutionContext Quartz 提供的排程執行上下文，可從中取得 JobDataMap 等參數
     * @throws Exception 子類別實作時可拋出任意例外，由 {@link #executeInternal} 統一捕捉處理
     */
    protected abstract void executeJob(JobExecutionContext jobExecutionContext) throws Exception;
}
