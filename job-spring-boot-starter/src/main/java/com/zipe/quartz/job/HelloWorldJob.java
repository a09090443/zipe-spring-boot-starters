package com.zipe.quartz.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author : Gary Tsai
 **/
@Slf4j
@Component
public class HelloWorldJob {
    @Scheduled(cron = "0/20 * * * * ?")
    public void helloWorld(){
        log.info("Hello World");
    }
}
