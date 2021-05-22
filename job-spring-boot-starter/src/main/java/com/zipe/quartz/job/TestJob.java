package com.zipe.quartz.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/26 下午 05:38
 **/
@Slf4j
public class TestJob extends QuartzJobFactory {

    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) throws Exception {
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        log.info("TestJob執行, 當前的時間: " + now);
    }
}
