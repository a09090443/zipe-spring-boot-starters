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

    /** 由 quartz-jobs.properties 檔案所建立的排程，統一使用此 group 名稱加以識別 */
    private final String JOB_GROUP_NAME = "file";

    /** Quartz 排程器，負責排程的新增、刪除與執行控制 */
    private final Scheduler scheduler;

    /** 從 quartz-jobs.properties 讀取的排程設定物件 */
    private final QuartzJobPropertyConfig quartzJobPropertyConfig;

    /**
     * 建構 {@code InitialJobAutoConfiguration}，透過 Spring 依賴注入取得排程器與排程設定。
     *
     * @param scheduler               Quartz 排程器實例
     * @param quartzJobPropertyConfig 排程屬性設定物件
     */
    public InitialJobAutoConfiguration(Scheduler scheduler,
                                       QuartzJobPropertyConfig quartzJobPropertyConfig) {
        this.scheduler = scheduler;
        this.quartzJobPropertyConfig = quartzJobPropertyConfig;
    }

    /**
     * 依照 {@link QuartzJobPropertyConfig} 中讀取到的排程設定，
     * 清除既有的 "schedule" 群組排程後，重新建立並啟動所有排程。
     * <p>
     * 每次應用程式啟動時執行，確保排程定義與設定檔保持一致。
     * </p>
     */
    @Bean
    public void createJobs() {
        quartzJobPropertyConfig.getJobMap().forEach((key, value) -> {
            // FIXME(待修 Bug)：此處刪除查詢的 group 為 "schedule"，但下方建立 job 使用的 group 為 JOB_GROUP_NAME="file"，
            //   兩者不一致，導致每次啟動無法清除舊的 "file" 群組排程，長期累積殭屍排程。應統一為同一 group。
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
            QuartzJobUtil quartzManageUtil = new QuartzJobUtil(job, quartzJobPropertyConfig.effectiveAllowedClasses());
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
