package com.zipe.db.autoconfiguration;

import com.zipe.base.aspect.DynamicDataSourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceAspectAutoConfiguration {
    @Bean
    public DynamicDataSourceAspect getDynamicDataSourceAspect() {
        return new DynamicDataSourceAspect();
    }
}
