package com.zipe.quartz.job;

import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * @author : Gary Tsai
 **/
@Slf4j
public abstract class QuartzJobFactory extends QuartzJobBean {

    /**
     * 排程執行前 Log 紀錄
     *
     * @param jobExecutionContext
     */
    protected void beforeExecuteJobLog(JobExecutionContext jobExecutionContext) {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        log.info("排程名稱:{} 執行開始:時間{}", jobName, DateTimeUtils.getDateNow());
    }
    /**
     * 排程執行後 Log 紀錄
     *
     * @param jobExecutionContext
     */
    protected void afterExecuteJobLog(JobExecutionContext jobExecutionContext) {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        log.info("排程名稱:{} 執行結束:時間{}", jobName, DateTimeUtils.getDateNow());
    }

    /**
     * 排程執行錯誤 Log 紀錄
     *
     * @param jobExecutionContext
     * @param e
     */
    protected void errorExecuteJobLog(JobExecutionContext jobExecutionContext, Exception e) {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        log.error("排程名稱:{} 發生錯誤，{}", jobName, e.getMessage());
    }

    /**
     * 執行排程
     *
     * @param jobExecutionContext
     * @throws JobExecutionException
     */
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        this.beforeExecuteJobLog(jobExecutionContext);
        try {
            executeJob(jobExecutionContext);
        } catch (Exception e) {
            this.errorExecuteJobLog(jobExecutionContext, e);
        }
        this.afterExecuteJobLog(jobExecutionContext);
    }

    /**
     * 排程預設定義的執行方法
     *
     * @param jobExecutionContext
     * @throws Exception
     */
    protected abstract void executeJob(JobExecutionContext jobExecutionContext) throws Exception;
}
