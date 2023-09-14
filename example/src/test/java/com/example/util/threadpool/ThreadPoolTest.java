package com.example.util.threadpool;

import com.example.base.TestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
public class ThreadPoolTest extends TestBase {

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Test
    void threadPoolTest() {
        String str = "submit方法";
        Future<String> future = threadPoolTaskExecutor.submit(new ThreadCallableTask(str));
        try {
            str = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        log.debug("主线程调用结束");
    }
}
