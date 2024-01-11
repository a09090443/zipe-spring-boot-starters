package com.zipe.quartz.base;

import com.zipe.quartz.enums.ScheduleEnum;
import com.zipe.quartz.enums.ScheduleJobStatusEnum;
import com.zipe.quartz.model.Job;
import com.zipe.quartz.util.QuartzJobUtil;
import com.zipe.quartz.vo.ScheduleJobVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Quartz job 管理
 *
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
     * 刪除排程
     *
     * @param scheduleJobDetail
     * @return
     */
    protected ScheduleJobVO deleteJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.DELETE);
        return scheduleJobDetail;
    }

    /**
     * 暫停排程
     *
     * @param scheduleJobDetail
     * @return
     */
    protected ScheduleJobVO pauseJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.PAUSE);
        return scheduleJobDetail;
    }

    /**
     * 回復排程
     *
     * @param scheduleJobDetail
     * @return
     */
    protected ScheduleJobVO resumeJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.RESUME);
        return scheduleJobDetail;
    }

    /**
     * 馬上執行一次性排程
     *
     * @param scheduleJobDetail
     * @return
     */
    protected ScheduleJobVO runJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.ONCE);
        return scheduleJobDetail;
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
                case ONCE:
                    ScheduleBuilder scheduleBuilder = ScheduleEnum.NOW.setCycle(scheduleJobDetail.getRepeatInterval());
                    scheduleJobDetail.setStartTime(null);
                    this.executeOnce(convertToJob(scheduleJobDetail), scheduleBuilder);
                    break;
                case MERGE:
                    if (StringUtils.isNotBlank(scheduleJobDetail.getCronExpression())) {
                        scheduleBuilder = ScheduleEnum.CRON.setExpression(scheduleJobDetail.getCronExpression());
                    } else {
                        scheduleBuilder = ScheduleEnum.getTimeUnit(scheduleJobDetail.getTimeUnit())
                                .setCycle(scheduleJobDetail.getRepeatInterval());
                    }
                    this.createJob(convertToJob(scheduleJobDetail), scheduleBuilder);
                    break;
                case PAUSE:
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

    /**
     * 建立 Job，會以覆蓋方式建立
     *
     * @param job
     * @param scheduleBuilder
     * @throws Exception
     */
    private void createJob(Job job, ScheduleBuilder scheduleBuilder) throws Exception {

        TriggerKey triggerKey = TriggerKey.triggerKey(job.getName(), job.getGroup());
        // 如已有存在觸發器，需先移除
        Optional<Trigger> trigger = Optional.ofNullable(scheduler.getTrigger(triggerKey));
        if (trigger.isPresent()) {
            try {
                scheduler.unscheduleJob(triggerKey);
            } catch (SchedulerException e) {
                log.error("Job key:{}, remove trigger error.", trigger.get().getKey());
            }
        }

        QuartzJobUtil quartzManageUtil = new QuartzJobUtil(job);
        JobDetail jobDetail = null;
        Trigger newTrigger = null;
        try {
            jobDetail = quartzManageUtil.buildJobDetail(job);
            newTrigger = quartzManageUtil.buildJobTrigger(scheduleBuilder);
        } catch (ClassNotFoundException e) {
            log.error("Job name:{}, class not found, class name:{}", job.getName(), job.getClazz());
        }

        Set<Trigger> set = new HashSet<>();
        set.add(newTrigger);
        try {
            // boolean replace 表示啟動時對資料庫中的quartz的任務進行覆蓋。
            scheduler.scheduleJob(jobDetail, set, true);
        } catch (SchedulerException e) {
            log.error("Job name:{}, created job error.", job.getName());
        }
        log.debug("Job created: {}", jobDetail.getKey());

    }

    /**
     * 執行一次性 Job
     *
     * @param job
     * @param scheduleBuilder
     * @throws Exception
     */
    private void executeOnce(Job job, ScheduleBuilder scheduleBuilder) throws Exception {
        job.setName(job.getName() + "-Once");
        createJob(job, scheduleBuilder);
    }

    private Job convertToJob(ScheduleJobVO scheduleJobDetail) {
        return new Job(scheduleJobDetail.getJobName(),
                scheduleJobDetail.getJobDescription(),
                scheduleJobDetail.getJobGroup(),
                scheduleJobDetail.getJobClass(),
                scheduleJobDetail.getCronExpression(), scheduleJobDetail.getStartTime(),
                scheduleJobDetail.getEndTime(),
                scheduleJobDetail.getJobDataMap());
    }
}
