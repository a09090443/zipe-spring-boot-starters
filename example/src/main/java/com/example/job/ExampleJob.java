package com.example.job;

import com.zipe.quartz.job.QuartzJobFactory;
import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

/**
 * @author Gary.Tsai
 */
@Slf4j
public class ExampleJob extends QuartzJobFactory {

    public ExampleJob() {
    }

    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) throws Exception {
        log.info("{}執行, 當前的時間:{}", this.getClass(), DateTimeUtils.getDateNow());
    }
}
