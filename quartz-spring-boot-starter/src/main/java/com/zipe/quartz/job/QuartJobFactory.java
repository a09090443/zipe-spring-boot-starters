package com.zipe.quartz.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/28 上午 09:08
 **/
@Slf4j
public abstract class QuartJobFactory extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        log.info("排程名稱:{} 執行開始", jobName);
        try{
            executeJob(jobExecutionContext);
        }catch (Exception e){
            log.error("排程名稱:{} 發生錯誤，{}", jobName, e.getMessage());
        }
        log.info("排程名稱:{} 執行結束", jobName);

    }

    protected abstract void executeJob (JobExecutionContext jobExecutionContext) throws Exception;
}
