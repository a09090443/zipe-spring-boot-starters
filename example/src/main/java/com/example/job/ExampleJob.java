package com.example.job;

import com.zipe.quartz.job.QuartJobFactory;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class ExampleJob extends QuartJobFactory {

    public ExampleJob() {
    }

    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) throws Exception {
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        log.info("TestJob執行, 當前的時間: " + now);
    }
}
