package com.zipe.autoconfiguration;

import com.zipe.config.QuartzPropertyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

/**
 * @author zipe
 */
@Configuration
@PropertySource({"classpath:quartz.properties"})
public class QuartzAutoConfiguration {

    private final QuartzPropertyConfig quartzPropertyConfig;

    @Autowired
    public QuartzAutoConfiguration(QuartzPropertyConfig quartzPropertyConfig) {
        this.quartzPropertyConfig = quartzPropertyConfig;
    }

    @Bean
    @ConditionalOnResource(resources = {"classpath:quartz.properties"})
    public void test() {
        System.out.println("test");
    }

    @Bean
    @ConditionalOnBean(name = "quartzDataSource")
    public SchedulerFactoryBean factoryBean(@Qualifier("quartzDataSource") DataSource dataSource,
                                            @Qualifier("quartzTransactionManager") PlatformTransactionManager quartzTransactionManager) throws IOException {
        final SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setDataSource(dataSource);
        scheduler.setTransactionManager(quartzTransactionManager);
        scheduler.setOverwriteExistingJobs(true);
        scheduler.setQuartzProperties(quartzProperties());
        return scheduler;
    }

    @Bean
    public Properties quartzProperties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/quartz.properties"));
        propertiesFactoryBean.afterPropertiesSet();
        return propertiesFactoryBean.getObject();
    }
}
