package com.zipe.config;

import com.zipe.job.TestJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.Objects;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 上午 10:16
 **/
@Configuration
public class QuartzConfig {

    private final JobFactory jobFactory;

    private final Environment env;

    @Autowired
    public QuartzConfig(JobFactory jobFactory, Environment env) {
        this.jobFactory = jobFactory;
        this.env = env;
    }

    @Bean
    public JobDetailFactoryBean testJob() {
        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
        jobDetailFactoryBean.setDurability(true);
        jobDetailFactoryBean.setJobClass(TestJob.class);
        return jobDetailFactoryBean;
    }

    //    @Bean
//    public JobDetailFactoryBean invoiceUnusedUploadJob() {
//        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
//        jobDetailFactoryBean.setDurability(true);
//        jobDetailFactoryBean.setJobClass(InvoiceUnusedUploadJob.class);
//        return jobDetailFactoryBean;
//    }
//
//    @Bean
//    public JobDetailFactoryBean turnkeyDownloadResultJob() {
//        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
//        jobDetailFactoryBean.setDurability(true);
//        jobDetailFactoryBean.setJobClass(TurnkeyDownloadResultJob.class);
//        return jobDetailFactoryBean;
//    }
//
    @Bean
    public CronTriggerFactoryBean invoiceUploadJobCornTrigger() {
        CronTriggerFactoryBean cronTriggerFactoryBean = new CronTriggerFactoryBean();
        cronTriggerFactoryBean.setJobDetail(Objects.requireNonNull(testJob().getObject()));
        cronTriggerFactoryBean.setCronExpression("*/5 * * * * ?");
        return cronTriggerFactoryBean;
    }

    //
//    @Bean
//    public CronTriggerFactoryBean invoiceUnusedUploadJobCornTrigger() {
//        CronTriggerFactoryBean cronTriggerFactoryBean = new CronTriggerFactoryBean();
//        cronTriggerFactoryBean.setJobDetail(Objects.requireNonNull(invoiceUnusedUploadJob().getObject()));
//        cronTriggerFactoryBean.setCronExpression("0 0 23 9 1,3,5,7,9,11 ?");
//        return cronTriggerFactoryBean;
//    }
//
//    @Bean
//    public CronTriggerFactoryBean turnkeyDownloadResultJobCornTrigger() {
//        CronTriggerFactoryBean cronTriggerFactoryBean = new CronTriggerFactoryBean();
//        cronTriggerFactoryBean.setJobDetail(Objects.requireNonNull(turnkeyDownloadResultJob().getObject()));
//        cronTriggerFactoryBean.setCronExpression("0 15,45 * * * ?");
//        return cronTriggerFactoryBean;
//    }
//
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setJobFactory(jobFactory);
        schedulerFactoryBean.setTriggers(invoiceUploadJobCornTrigger().getObject());
        return schedulerFactoryBean;
    }
}
