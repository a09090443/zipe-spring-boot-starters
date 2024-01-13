package com.example.job;

import com.zipe.quartz.job.QuartzJobFactory;
import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

import java.util.Map;

@Slf4j
public class ExampleDbJob extends QuartzJobFactory {
    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) {
        Map<String, Object> jobMap = jobExecutionContext.getJobDetail().getJobDataMap();
        log.info("{}執行, 當前的時間:{}", this.getClass(), DateTimeUtils.getDateNow());
        jobMap.forEach(
                (k, v) -> log.info("參數名稱{}, 參數內容{}", k, v)
        );
    }
}
