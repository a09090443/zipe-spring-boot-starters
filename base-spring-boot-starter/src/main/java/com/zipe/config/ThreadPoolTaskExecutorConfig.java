package com.zipe.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadPoolTaskExecutorConfig {
    public static int CORE_POOL_SIZE = 5;
    public static int MAX_POOL_SIZE = 1000;

}