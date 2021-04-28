package com.zipe.quartz.autoconfiguration;

import com.zipe.quartz.config.QuartzJobPropertyConfig;
import com.zipe.quartz.enums.ScheduleEnum;
import com.zipe.quartz.model.Job;
import com.zipe.quartz.util.QuartzJobUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashSet;
import java.util.Set;

/**
 * 從 quartz-jobs.properties 自動建立排程
 *
 * @author zipe
 */
@Slf4j
@Configuration
@ConditionalOnClass(QuartzJobPropertyConfig.class)
@EnableConfigurationProperties(QuartzJobPropertyConfig.class)
@PropertySource(value = {"classpath:quartz-jobs.properties"}, encoding = "UTF-8")
public class InitialJobAutoConfiguration {

    private final Scheduler scheduler;

    private final QuartzJobPropertyConfig quartzJobPropertyConfig;

    @Autowired
    public InitialJobAutoConfiguration(Scheduler scheduler,
                                       QuartzJobPropertyConfig quartzJobPropertyConfig) {
        this.scheduler = scheduler;
        this.quartzJobPropertyConfig = quartzJobPropertyConfig;
    }

    @Bean
    public void createJobs() {
        quartzJobPropertyConfig.getJobMap().entrySet().stream().forEach(e -> {
            Job job = new Job();
            job.setName(e.getValue().getName());
            job.setClazz(e.getValue().getClazz());
            job.setGroup(e.getValue().getGroup());
            job.setCronExpression(e.getValue().getCronExpression());
            QuartzJobUtil quartzManageUtil = new QuartzJobUtil(job);
            try {
                JobDetail detail = quartzManageUtil.buildJobDetail();
                Trigger trigger = quartzManageUtil.buildJobTrigger(ScheduleEnum.CRON.setExpression(job.getCronExpression()));

                Set<Trigger> set = new HashSet<>();
                set.add(trigger);
                // boolean replace 表示啟動時對資料庫中的quartz的任務進行覆蓋。
                scheduler.scheduleJob(detail, set, true);
            } catch (ClassNotFoundException classNotFoundException) {
                log.error("Job's name : {}, cloud not find class, {}", e.getKey(), classNotFoundException.getMessage());
            } catch (SchedulerException schedulerException) {
                log.error("Job's name : {}, created error, {}", e.getKey(), schedulerException.getMessage());
            }
        });
    }
}
