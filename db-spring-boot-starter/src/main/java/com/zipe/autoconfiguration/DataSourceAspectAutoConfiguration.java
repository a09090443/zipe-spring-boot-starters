package com.zipe.autoconfiguration;

import com.zipe.base.aspect.DynamicDataSourceAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/14 下午 05:52
 **/
@AutoConfiguration
public class DataSourceAspectAutoConfiguration {
    @Bean
    public DynamicDataSourceAspect getDynamicDataSourceAspect() {
        return new DynamicDataSourceAspect();
    }
}
