package com.zipe.quartz.autoconfiguration;

import com.zipe.quartz.config.QuartzJobPropertyConfig;
import com.zipe.quartz.controller.QuartzController;
import com.zipe.quartz.enums.ScheduleEnum;
import com.zipe.quartz.model.Job;
import com.zipe.quartz.util.QuartzJobUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 從 quartz-jobs.properties 自動建立排程
 *
 * @author zipe
 */
@Slf4j
@AutoConfiguration
@EnableScheduling
@ConditionalOnClass(QuartzJobPropertyConfig.class)
@EnableConfigurationProperties(QuartzJobPropertyConfig.class)
@Import(value = {QuartzController.class})
@PropertySource(value = {"classpath:quartz.properties", "classpath:quartz-jobs.properties", "classpath:spring-quartz.properties"}, encoding = "UTF-8")
@ConditionalOnProperty(name = "spring.quartz.enable", havingValue = "true")
public class InitialJobAutoConfiguration {

    private final String JOB_GROUP_NAME = "file";

    private final Scheduler scheduler;

    private final QuartzJobPropertyConfig quartzJobPropertyConfig;

    public InitialJobAutoConfiguration(Scheduler scheduler,
                                       QuartzJobPropertyConfig quartzJobPropertyConfig) {
        this.scheduler = scheduler;
        this.quartzJobPropertyConfig = quartzJobPropertyConfig;
    }

    @Bean
    public void createJobs() {
        quartzJobPropertyConfig.getJobMap().forEach((key, value) -> {
            GroupMatcher<JobKey> matcher = GroupMatcher.jobGroupEquals("schedule");
            try {
                Set<JobKey> jobKeys = scheduler.getJobKeys(matcher);
                scheduler.deleteJobs(new ArrayList<>(jobKeys));
            } catch (SchedulerException schedulerException) {
                schedulerException.printStackTrace();
            }

            Job job = new Job();
            job.setName(value.getName());
            job.setClazz(value.getClazz());
            // 由 quartz-jobs.properties 所產生的 job 統一的 group name 為 "file"
            job.setGroup(JOB_GROUP_NAME);
            job.setCronExpression(value.getCronExpression());
            QuartzJobUtil quartzManageUtil = new QuartzJobUtil(job);
            try {
                JobDetail detail = quartzManageUtil.buildJobDetail();
                Trigger trigger = quartzManageUtil.buildJobTrigger(ScheduleEnum.CRON.setExpression(job.getCronExpression()));

                Set<Trigger> set = new HashSet<>();
                set.add(trigger);
                // boolean replace 表示啟動時對資料庫中的quartz的任務進行覆蓋。
                scheduler.scheduleJob(detail, set, true);
            } catch (ClassNotFoundException classNotFoundException) {
                log.error("Job's name : {}, cloud not find class, {}", key, classNotFoundException.getMessage());
            } catch (SchedulerException schedulerException) {
                log.error("Job's name : {}, created error, {}", key, schedulerException.getMessage());
            }
        });
    }

}
