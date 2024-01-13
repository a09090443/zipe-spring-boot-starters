package com.zipe.quartz.util;

import com.zipe.quartz.model.Job;
import com.zipe.util.time.DateTimeUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.Date;
import java.util.Optional;

/**
 * Quartz 建立 Job 工具
 **/
public class QuartzJobUtil {

    private Job job;

    public QuartzJobUtil() {
    }

    public QuartzJobUtil(Job job) {
        this.job = job;
    }

    public JobDetail buildJobDetail() throws ClassNotFoundException {
        return this.buildJobDetail(this.job);
    }

    public JobDetail buildJobDetail(Job job) throws ClassNotFoundException {
        Class clazz = Class.forName(job.getClazz());

        return JobBuilder
                .newJob(clazz)
                .withIdentity(job.getName(), job.getGroup())
                .withDescription(job.getDescription())
                .usingJobData(job.getDataMap())
                .storeDurably()
                .build();
    }

    public Trigger buildJobTrigger(ScheduleBuilder builder) throws ClassNotFoundException {
        return this.buildJobTrigger(this.buildJobDetail(), this.job, builder);
    }

    public Trigger buildJobTrigger(JobDetail jobDetail, Job job, ScheduleBuilder builder) {
        Date startDate = Optional.ofNullable(job.getStartTime()).isPresent() ? DateTimeUtils.localDateTimeToDate(job.getStartTime()) : new Date();
        Date endDate = Optional.ofNullable(job.getEndTime()).isPresent() ? DateTimeUtils.localDateTimeToDate(job.getEndTime()) : null;
        return TriggerBuilder
                .newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
                .withDescription(job.getDescription())
                .startAt(startDate)
                .endAt(endDate)
                .withSchedule(builder)
                .build();
    }

}
