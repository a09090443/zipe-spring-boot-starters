package com.zipe.quartz.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldJob {
    @Scheduled(cron = "0/20 * * * * ?")
    public void helloWorld(){
        System.out.println("Hello World!!");
    }
}
