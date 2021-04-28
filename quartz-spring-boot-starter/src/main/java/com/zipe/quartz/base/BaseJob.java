package com.zipe.quartz.base;

import com.zipe.quartz.enums.ScheduleEnum;
import com.zipe.quartz.enums.ScheduleJobStatusEnum;
import com.zipe.quartz.vo.ScheduleJobVO;
import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.UUID;

/**
 * Quartz job 管理
 * @author : Gary Tsai
 * @created : @Date 2021/4/28 上午 09:43
 **/
@Slf4j
public abstract class BaseJob {

    private final Scheduler scheduler;

    @Autowired
    public BaseJob(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 新增或更新排程
     *
     * @param scheduleJobDetail
     * @return
     */
    protected ScheduleJobVO mergeJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.MERGE);
        return scheduleJobDetail;
    }

    private ScheduleJobVO scheduleJobStatusProcess(
            ScheduleJobVO scheduleJobDetail, ScheduleJobStatusEnum scheduleJobStatusEnum) {
        JobKey jobKey = JobKey.jobKey(scheduleJobDetail.getJobName(), scheduleJobDetail.getJobGroup());

        try {
            switch (scheduleJobStatusEnum) {
                case MERGE:
                    Date nextFireTime = DateTimeUtils.localDateTimeToDate(scheduleJobDetail.getNextFireTime());

                    if (!scheduler.checkExists(jobKey)) {
                        this.createJob(nextFireTime, scheduleJobDetail);
                    } else {
                        this.updateTrigger(jobKey, nextFireTime, scheduleJobDetail);
                    }

                    break;

                case SUSPEND:
                    scheduler.pauseJob(jobKey);
                    break;

                case RESUME:
                    scheduler.resumeJob(jobKey);
                    break;

                case DELETE:
                    scheduler.deleteJob(jobKey);
                    break;
            }
        } catch (Exception e) {
            scheduleJobDetail.setMessage("排程: " + scheduleJobDetail.getJobName() + "發生未知錯誤。");
            log.error(e.getMessage());
        }

        return scheduleJobDetail;
    }

    private ScheduleJobVO createJob(Date nextFireTime, ScheduleJobVO scheduleJobDetail) throws Exception {
        JobDetail jobDetail = buildJobDetail(scheduleJobDetail);
        Trigger trigger = buildJobTrigger(jobDetail, nextFireTime, scheduleJobDetail);
        scheduler.scheduleJob(jobDetail, trigger);
        log.debug("Job created: " + jobDetail);

        return scheduleJobDetail;
    }

    private JobDetail buildJobDetail(ScheduleJobVO scheduleJobDetail) throws Exception {
        Class clazz = Class.forName(scheduleJobDetail.getJobClass());

        if (null == scheduleJobDetail.getJobDataMap()) {
            scheduleJobDetail.setJobDataMap(new JobDataMap());
        }

        return JobBuilder
                .newJob(clazz)
                .withIdentity(scheduleJobDetail.getJobName(), scheduleJobDetail.getJobGroup())
                .withDescription(scheduleJobDetail.getJobDescription())
                .usingJobData(scheduleJobDetail.getJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(
            JobDetail jobDetail, Date startTime, ScheduleJobVO scheduleJobDetail) {
        return TriggerBuilder
                .newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
                .withDescription(scheduleJobDetail.getJobDescription())
                .startAt(startTime)
                .endAt(null)
                .withSchedule(ScheduleEnum.getTimeUnit(scheduleJobDetail.getTimeUnit())
                        .setCycle(scheduleJobDetail.getRepeatInterval(), scheduleJobDetail.getExecuteTimes()))
                .build();
    }

    private void updateTrigger(JobKey jobKey, Date nextFireTime, ScheduleJobVO newDetail) throws Exception {
        JobDetail oriDetail = scheduler.getJobDetail(jobKey);
        TriggerKey triggerKey = TriggerKey.triggerKey(oriDetail.getKey().getName(), oriDetail.getKey().getGroup());
        Trigger trigger = scheduler.getTrigger(triggerKey);

        /*
         * 若是在編輯畫面選擇執行頻率「一次」的話會導致舊的Trigger因為已經跑完而銷毀,
         * 且已銷毀Trigger的JobDetail無法refresh新的Trigger,
         * 所以直接砍掉重建。
         */
        if (trigger == null) {
            scheduler.deleteJob(jobKey);
            createJob(nextFireTime, newDetail);
        } else {
            trigger = trigger
                    .getTriggerBuilder()
                    .withIdentity(triggerKey)
                    .startAt(nextFireTime)
                    .endAt(null)
                    .withSchedule(ScheduleEnum.getTimeUnit(newDetail.getTimeUnit())
                            .setCycle(newDetail.getRepeatInterval(), newDetail.getExecuteTimes()))
                    .build();
            scheduler.rescheduleJob(triggerKey, trigger);
            log.debug("Job existed but update trigger: " + trigger);
        }
    }

    private void executeOnce(JobKey jobKey, ScheduleJobVO scheduleJobDetail) throws Exception {
        JobDetail detail = scheduler.getJobDetail(jobKey);

        // 若是新的排程則新建一筆資料進quartz table。
        if (detail == null) {
            createJob(new Date(), scheduleJobDetail);
            detail = scheduler.getJobDetail(jobKey);
        }

        Trigger newTrigger = TriggerBuilder
                .newTrigger()
                .startNow()
                .forJob(detail)
                .withIdentity(UUID.randomUUID().toString())
                .withDescription(scheduleJobDetail.getJobDescription())
                .withSchedule(SimpleScheduleBuilder.repeatSecondlyForTotalCount(1, 1))
                .build();

        scheduler.scheduleJob(newTrigger);
    }

}
