package com.zipe.quartz.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 示範用排程任務，每 20 秒執行一次並輸出 "Hello World" 日誌。
 * <p>
 * 此類別作為 Quartz / Spring Scheduled 的最小可執行範例，
 * 供開發人員確認排程框架是否正確啟動。
 * </p>
 *
 * @author : Gary Tsai
 **/
@Slf4j
@Component
public class HelloWorldJob {

    /**
     * 每 20 秒觸發一次的示範排程方法，執行時僅輸出一行 INFO 日誌。
     * <p>
     * Cron 表示式 {@code 0/20 * * * * ?} 代表從第 0 秒起、每隔 20 秒執行一次。
     * </p>
     */
    @Scheduled(cron = "0/20 * * * * ?")
    public void helloWorld(){
        log.info("Hello World");
    }
}
