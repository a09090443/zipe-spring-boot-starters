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

    /**
     * 建立不帶 Job 與白名單的預設實例。
     * 通常作為框架層注入後再透過 setter 設定 Job 資訊。
     */
    public QuartzJobUtil() {
        this.allowedClasses = Collections.emptySet();
    }

    /**
     * 以指定的 Job 資訊建立實例，白名單預設為空集合。
     *
     * @param job 排程設定資料模型
     */
    public QuartzJobUtil(Job job) {
        this(job, Collections.emptySet());
    }

    /**
     * 以指定的 Job 資訊與白名單建立實例。
     *
     * @param job            排程設定資料模型
     * @param allowedClasses 允許載入的 Job 類別全名集合；傳入 {@code null} 時視同空集合
     */
    public QuartzJobUtil(Job job, Set<String> allowedClasses) {
        this.job = job;
        this.allowedClasses = allowedClasses == null ? Collections.emptySet() : allowedClasses;
    }

    /**
     * 依目前持有的 {@link Job} 資訊建立 {@link JobDetail}。
     *
     * @return 建立完成的 {@link JobDetail} 實例
     * @throws ClassNotFoundException 找不到 Job 類別時拋出
     */
    public JobDetail buildJobDetail() throws ClassNotFoundException {
        return this.buildJobDetail(this.job);
    }

    /**
     * 依傳入的 {@link Job} 資訊建立 {@link JobDetail}，
     * 包含 Job 識別、說明、資料映射（JobDataMap）等設定。
     *
     * @param job 排程設定資料模型
     * @return 建立完成的 {@link JobDetail} 實例
     * @throws ClassNotFoundException 找不到 Job 類別時拋出
     */
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

    /**
     * 依目前持有的 {@link Job} 資訊與排程規則建立 {@link Trigger}。
     * 內部會先呼叫 {@link #buildJobDetail()} 取得 {@link JobDetail}。
     *
     * @param builder 排程規則建構器（如 CronScheduleBuilder 或 SimpleScheduleBuilder）
     * @return 建立完成的 {@link Trigger} 實例
     * @throws ClassNotFoundException 找不到 Job 類別時拋出
     */
    public Trigger buildJobTrigger(ScheduleBuilder builder) throws ClassNotFoundException {
        return this.buildJobTrigger(this.buildJobDetail(), this.job, builder);
    }

    /**
     * 依傳入的 {@link JobDetail}、{@link Job} 資訊與排程規則建立 {@link Trigger}，
     * 包含觸發器識別、說明、起始時間及結束時間設定。
     *
     * @param jobDetail 已建立的 {@link JobDetail} 實例
     * @param job       排程設定資料模型，提供起始/結束時間與說明
     * @param builder   排程規則建構器（如 CronScheduleBuilder 或 SimpleScheduleBuilder）
     * @return 建立完成的 {@link Trigger} 實例
     */
    public Trigger buildJobTrigger(JobDetail jobDetail, Job job, ScheduleBuilder builder) {
        // 若 startTime 為 null，則以當下時間作為觸發起始點
        Date startDate = Optional.ofNullable(job.getStartTime()).isPresent() ? DateTimeUtils.localDateTimeToDate(job.getStartTime()) : new Date();
        // 若 endTime 為 null，表示排程永不結束
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
