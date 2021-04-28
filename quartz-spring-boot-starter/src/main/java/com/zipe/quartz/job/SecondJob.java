package com.zipe.quartz.job;

import com.zipe.quartz.service.TestService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class SecondJob extends QuartJobFactory {

    private final TestService testServiceImpl;

    SecondJob(TestService testServiceImpl){
        this.testServiceImpl = testServiceImpl;
    }

    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) throws Exception {
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        testServiceImpl.test();
        log.info("SecondJob執行, 當前的時間: " + now);
    }
}
