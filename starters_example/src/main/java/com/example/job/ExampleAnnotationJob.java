package com.example.job;

import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Gary.Tsai
 */
@Slf4j
@Component
public class ExampleAnnotationJob {

    @Scheduled(cron = "0/20 * * * * ?")
    public void example() {
        log.info("{}執行, 當前的時間:{}", this.getClass(), DateTimeUtils.getDateNow());
    }
}
