package com.example.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Gary.Tsai
 */
@Component
public class ExampleAnnotationJob {

    @Scheduled(cron = "0/20 * * * * ?")
    public void example(){
        System.out.println("example");
    }
}
