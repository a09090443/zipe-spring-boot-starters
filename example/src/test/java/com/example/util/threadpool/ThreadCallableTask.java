package com.example.util.threadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
public class ThreadCallableTask implements Callable<String> {

    private String name;

    public ThreadCallableTask(String name) {
        this.name = name;
    }

    @Override
    public String call() throws Exception {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        name = "MyOrderCallableTask";
        log.debug("MyOrderCallableTask已执行");
        return name;
    }
}
