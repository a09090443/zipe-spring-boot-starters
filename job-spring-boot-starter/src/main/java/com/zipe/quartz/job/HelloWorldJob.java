package com.zipe.quartz.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/28 下午 02:57
 **/
@Component
public class HelloWorldJob {
    @Scheduled(cron = "0/20 * * * * ?")
    public void helloWorld(){
        System.out.println("Hello World!!");
    }
}
