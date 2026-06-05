package com.example.job;

import com.zipe.quartz.job.QuartzJobFactory;
import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

@Slf4j
public class ExampleXmlJob extends QuartzJobFactory {
    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) {
        log.info("{}執行, 當前的時間:{}", this.getClass(), DateTimeUtils.getDateNow());
    }
}
