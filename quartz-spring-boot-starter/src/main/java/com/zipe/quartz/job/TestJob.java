package com.zipe.quartz.job;

import com.zipe.quartz.service.TestService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/26 下午 05:38
 **/
@Slf4j
public class TestJob extends QuartJobFactory {

    private final TestService testServiceImpl;

    TestJob(TestService testServiceImpl){
        this.testServiceImpl = testServiceImpl;
    }

    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) throws Exception {
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        testServiceImpl.test();
        log.info("TestJob執行, 當前的時間: " + now);
    }
}
