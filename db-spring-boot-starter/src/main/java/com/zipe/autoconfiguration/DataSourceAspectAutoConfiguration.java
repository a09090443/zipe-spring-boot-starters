package com.zipe.autoconfiguration;

import com.zipe.base.aspect.DynamicDataSourceAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 動態資料來源 AOP 切面的自動配置類別。
 *
 * <p>負責將 {@link DynamicDataSourceAspect} 以 Spring Bean 的形式註冊至應用程式情境，
 * 使框架能夠攔截帶有 {@code @DS} 或 {@code @DynamicDS} 標註的方法，
 * 並在方法執行前後自動切換對應的資料來源。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/14 下午 05:52
 **/
@AutoConfiguration
public class DataSourceAspectAutoConfiguration {

    /**
     * 建立並註冊 {@link DynamicDataSourceAspect} Bean。
     *
     * <p>該切面會透過 AOP 攔截標有資料來源切換標註的方法，
     * 在方法執行前將目標資料來源寫入 {@code DataSourceHolder}（ThreadLocal），
     * 並於方法結束後清除，以防止資料來源污染。</p>
     *
     * @return 已初始化的 {@link DynamicDataSourceAspect} 實例
     */
    @Bean
    public DynamicDataSourceAspect getDynamicDataSourceAspect() {
        return new DynamicDataSourceAspect();
    }
}
