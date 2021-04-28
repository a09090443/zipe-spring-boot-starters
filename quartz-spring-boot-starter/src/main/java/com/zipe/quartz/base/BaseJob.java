package com.zipe.quartz.base;

import com.zipe.quartz.enums.ScheduleEnum;
import com.zipe.quartz.enums.ScheduleJobStatusEnum;
import com.zipe.quartz.model.Job;
import com.zipe.quartz.util.QuartzJobUtil;
import com.zipe.quartz.vo.ScheduleJobVO;
import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
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
        ScheduleBuilder scheduleBuilder = ScheduleEnum.getTimeUnit(scheduleJobDetail.getTimeUnit())
                .setCycle(scheduleJobDetail.getRepeatInterval(), scheduleJobDetail.getExecuteTimes());

        Job job = new Job(scheduleJobDetail.getJobName(),
                scheduleJobDetail.getJobDescription(),
                scheduleJobDetail.getJobGroup(),
                scheduleJobDetail.getJobClass(),
                scheduleJobDetail.getCronExpression(), DateTimeUtils.localDateTimeToDate(scheduleJobDetail.getStartTime()),
                DateTimeUtils.localDateTimeToDate(scheduleJobDetail.getEndTime()),
                scheduleJobDetail.getJobDataMap());

        if (StringUtils.isNotBlank(scheduleJobDetail.getCronExpression())) {
            scheduleBuilder = ScheduleEnum.CRON.setExpression(scheduleJobDetail.getCronExpression());
        }

        try {
            switch (scheduleJobStatusEnum) {
                case MERGE:
                    if (!scheduler.checkExists(jobKey)) {
                        this.createJob(job, scheduleBuilder);
                    } else {
                        this.updateTrigger(jobKey, job, scheduleBuilder);
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
                case ONCE:
                    scheduleBuilder = ScheduleEnum.NOW.setCycle(scheduleJobDetail.getRepeatInterval(), scheduleJobDetail.getExecuteTimes());
                    this.executeOnce(jobKey, job, scheduleBuilder);
            }
        } catch (Exception e) {
            scheduleJobDetail.setMessage("排程: " + scheduleJobDetail.getJobName() + "發生未知錯誤。");
            log.error(e.getMessage());
        }

        return scheduleJobDetail;
    }

    private void createJob(Job job, ScheduleBuilder scheduleBuilder) throws Exception {
        QuartzJobUtil quartzManageUtil = new QuartzJobUtil(job);
        JobDetail jobDetail = quartzManageUtil.buildJobDetail(job);
        Trigger trigger = quartzManageUtil.buildJobTrigger(scheduleBuilder);
        Set<Trigger> set = new HashSet<>();
        set.add(trigger);
        // boolean replace 表示啟動時對資料庫中的quartz的任務進行覆蓋。
        scheduler.scheduleJob(jobDetail, set, true);
        log.debug("Job created: {}", jobDetail.getKey());
    }

    private void updateTrigger(JobKey jobKey, Job job, ScheduleBuilder scheduleBuilder) throws Exception {
        JobDetail oriDetail = scheduler.getJobDetail(jobKey);
        TriggerKey triggerKey = TriggerKey.triggerKey(oriDetail.getKey().getName(), oriDetail.getKey().getGroup());
        Trigger trigger = scheduler.getTrigger(triggerKey);

        createJob(job, scheduleBuilder);

        log.debug("Job existed but update trigger: {}", trigger.getJobKey());
    }

    private void executeOnce(JobKey jobKey, Job job, ScheduleBuilder scheduleBuilder) throws Exception {
        job.setName(job.getName() + "-Once");
        createJob(job, scheduleBuilder);
    }

}
