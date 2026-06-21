package com.zipe.config;

import org.springframework.context.annotation.Configuration;

/**
 * 執行緒池（ThreadPoolTaskExecutor）全域設定類別。
 *
 * <p>定義應用程式共用執行緒池的核心參數常數，
 * 供各模組在建立 {@code ThreadPoolTaskExecutor} 時統一引用，
 * 避免各處硬編碼導致設定不一致。</p>
 */
@Configuration
public class ThreadPoolTaskExecutorConfig {

    /** 核心執行緒數：執行緒池維持的最小常駐執行緒數量，即使執行緒處於閒置狀態也不會被回收。 */
    public static int CORE_POOL_SIZE = 5;

    /** 最大執行緒數：當任務佇列已滿時，執行緒池可擴展至的執行緒數量上限。 */
    public static int MAX_POOL_SIZE = 1000;

}
