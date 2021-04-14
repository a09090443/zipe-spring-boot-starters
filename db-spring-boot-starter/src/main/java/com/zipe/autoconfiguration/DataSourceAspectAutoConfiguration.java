package com.zipe.autoconfiguration;

import com.zipe.base.aspect.DynamicDataSourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/14 下午 05:52
 **/
@Configuration
public class DataSourceAspectAutoConfiguration {
    @Bean
    public DynamicDataSourceAspect getDynamicDataSourceAspect() {
        return new DynamicDataSourceAspect();
    }
}
