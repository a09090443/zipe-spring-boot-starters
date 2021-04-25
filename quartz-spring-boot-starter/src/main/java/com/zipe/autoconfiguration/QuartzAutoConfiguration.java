package com.zipe.autoconfiguration;

import com.zipe.config.QuartzPropertyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author zipe
 */
@Configuration
@PropertySource({"classpath:quartz.properties"})
@ConditionalOnClass(QuartzPropertyConfig.class)
@EnableConfigurationProperties(QuartzPropertyConfig.class)
public class QuartzAutoConfiguration {

    private final QuartzPropertyConfig quartzPropertyConfig;

    @Autowired
    public QuartzAutoConfiguration(QuartzPropertyConfig quartzPropertyConfig) {
        this.quartzPropertyConfig = quartzPropertyConfig;
    }

    @Bean
    @ConditionalOnResource(resources = {"classpath:quartz.properties"})
    public void test(){
        System.out.println("test");
    }
}
