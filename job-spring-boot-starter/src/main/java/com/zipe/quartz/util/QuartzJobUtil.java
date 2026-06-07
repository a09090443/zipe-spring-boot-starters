package com.zipe.quartz.util;

import com.zipe.quartz.model.Job;
import com.zipe.util.time.DateTimeUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

/**
 * Quartz 建立 Job 工具。
 *
 * <p>為避免透過任意類別名稱載入造成 RCE，僅允許白名單（allowedClasses）內、
 * 且實作 {@link org.quartz.Job} 的類別被載入。白名單以外或型別不符者一律拒絕。</p>
 *
 * @author : Gary Tsai
 **/
public class QuartzJobUtil {

    private Job job;

    /** 允許載入的 Job 類別全名白名單。 */
    private final Set<String> allowedClasses;

    public QuartzJobUtil() {
        this.allowedClasses = Collections.emptySet();
    }

    public QuartzJobUtil(Job job) {
        this(job, Collections.emptySet());
    }

    public QuartzJobUtil(Job job, Set<String> allowedClasses) {
        this.job = job;
        this.allowedClasses = allowedClasses == null ? Collections.emptySet() : allowedClasses;
    }

    public JobDetail buildJobDetail() throws ClassNotFoundException {
        return this.buildJobDetail(this.job);
    }

    public JobDetail buildJobDetail(Job job) throws ClassNotFoundException {
        Class<? extends org.quartz.Job> clazz = resolveJobClass(job.getClazz());

        return JobBuilder
                .newJob(clazz)
                .withIdentity(job.getName(), job.getGroup())
                .withDescription(job.getDescription())
                .usingJobData(job.getDataMap())
                .storeDurably()
                .build();
    }

    /**
     * 解析並驗證 Job 類別：必須在白名單內，且實作 {@link org.quartz.Job}。
     *
     * @param className 類別全名
     * @return 已驗證的 Job 類別
     * @throws SecurityException      類別不在白名單或未實作 org.quartz.Job
     * @throws ClassNotFoundException 找不到類別
     */
    private Class<? extends org.quartz.Job> resolveJobClass(String className) throws ClassNotFoundException {
        if (className == null || !allowedClasses.contains(className)) {
            throw new SecurityException("Job class not in allow-list: " + className);
        }
        Class<?> clazz = Class.forName(className);
        if (!org.quartz.Job.class.isAssignableFrom(clazz)) {
            throw new SecurityException("Job class does not implement org.quartz.Job: " + className);
        }
        return clazz.asSubclass(org.quartz.Job.class);
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
