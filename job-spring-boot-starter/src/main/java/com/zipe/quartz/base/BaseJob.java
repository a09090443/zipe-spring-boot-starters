package com.zipe.quartz.base;

import com.zipe.quartz.config.QuartzJobPropertyConfig;
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
 * Quartz 排程管理基礎類別。
 *
 * <p>提供新增、更新、刪除、暫停、恢復及立即執行一次性排程等操作的通用實作。
 * 業務系統應繼承此類別並注入所需的 {@link Scheduler} 與
 * {@link QuartzJobPropertyConfig}，再透過各 {@code *JobProcess} 方法
 * 對 Quartz 排程進行生命週期管理。</p>
 *
 * @author : Gary Tsai
 **/
@Slf4j
public abstract class BaseJob {

    /** Quartz 排程器，負責實際執行排程操作（新增、暫停、恢復、刪除等）。 */
    private final Scheduler scheduler;

    /** Quartz 排程屬性設定，包含允許載入的 Job 類別白名單等設定。 */
    private final QuartzJobPropertyConfig propertyConfig;

    /**
     * 建構子，透過 Spring 依賴注入取得排程器與屬性設定。
     *
     * @param scheduler      Quartz {@link Scheduler} 實例
     * @param propertyConfig Quartz 排程屬性設定
     */
    @Autowired
    protected BaseJob(Scheduler scheduler, QuartzJobPropertyConfig propertyConfig) {
        this.scheduler = scheduler;
        this.propertyConfig = propertyConfig;
    }

    /**
     * 刪除排程。
     *
     * <p>將指定排程從 Quartz Scheduler 中移除，移除後無法再恢復。</p>
     *
     * @param scheduleJobDetail 排程詳細資訊，須包含 jobName 與 jobGroup
     * @return 處理後的排程詳細資訊（若發生錯誤，message 欄位會設置錯誤說明）
     */
    protected ScheduleJobVO deleteJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.DELETE);
        return scheduleJobDetail;
    }

    /**
     * 暫停排程。
     *
     * <p>將指定排程暫停，排程不會被移除，可透過 {@link #resumeJobProcess} 恢復執行。</p>
     *
     * @param scheduleJobDetail 排程詳細資訊，須包含 jobName 與 jobGroup
     * @return 處理後的排程詳細資訊（若發生錯誤，message 欄位會設置錯誤說明）
     */
    protected ScheduleJobVO pauseJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.PAUSE);
        return scheduleJobDetail;
    }

    /**
     * 恢復排程。
     *
     * <p>將先前暫停的排程重新啟動，繼續依原定排程週期執行。</p>
     *
     * @param scheduleJobDetail 排程詳細資訊，須包含 jobName 與 jobGroup
     * @return 處理後的排程詳細資訊（若發生錯誤，message 欄位會設置錯誤說明）
     */
    protected ScheduleJobVO resumeJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.RESUME);
        return scheduleJobDetail;
    }

    /**
     * 立即執行一次性排程。
     *
     * <p>以 {@code NOW} 模式建立一個僅執行一次的臨時排程，
     * Job 名稱會自動加上 {@code -Once} 後綴以避免與常駐排程衝突。</p>
     *
     * @param scheduleJobDetail 排程詳細資訊，須包含 jobName、jobGroup 及 repeatInterval
     * @return 處理後的排程詳細資訊（若發生錯誤，message 欄位會設置錯誤說明）
     */
    protected ScheduleJobVO runJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.ONCE);
        return scheduleJobDetail;
    }

    /**
     * 新增或更新排程。
     *
     * <p>若排程已存在則覆蓋（replace），若不存在則新增。
     * 當 {@code cronExpression} 不為空時優先使用 CRON 模式；
     * 否則依 {@code timeUnit} 與 {@code repeatInterval} 建立固定間隔排程。</p>
     *
     * @param scheduleJobDetail 排程詳細資訊，須包含 jobName、jobGroup、jobClass
     *                          及 cronExpression 或 timeUnit + repeatInterval
     * @return 處理後的排程詳細資訊（若發生錯誤，message 欄位會設置錯誤說明）
     */
    protected ScheduleJobVO mergeJobProcess(ScheduleJobVO scheduleJobDetail) {
        scheduleJobDetail = this.scheduleJobStatusProcess(scheduleJobDetail, ScheduleJobStatusEnum.MERGE);
        return scheduleJobDetail;
    }

    /**
     * 排程狀態處理的核心方法，根據傳入的 {@link ScheduleJobStatusEnum} 執行對應的 Quartz 操作。
     *
     * <p>此方法集中處理所有排程操作（立即執行、新增/更新、暫停、恢復、刪除），
     * 各公開方法皆委派至此處，統一進行錯誤捕捉與訊息設置。</p>
     *
     * @param scheduleJobDetail      排程詳細資訊
     * @param scheduleJobStatusEnum  要執行的排程操作類型
     * @return 操作後的排程詳細資訊，若發生例外則 message 欄位會記錄錯誤說明
     */
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
                    scheduleBuilder = ScheduleEnum.getTimeUnit(scheduleJobDetail.getTimeUnit())
                            .setCycle(scheduleJobDetail.getRepeatInterval());

                    if (StringUtils.isNotBlank(scheduleJobDetail.getCronExpression())) {
                        scheduleBuilder = ScheduleEnum.CRON.setExpression(scheduleJobDetail.getCronExpression());
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
     * 建立排程 Job，若已存在則以覆蓋方式重建。
     *
     * <p>先嘗試移除既有的 {@link Trigger}，再透過 {@link QuartzJobUtil}
     * 產生新的 {@link JobDetail} 與 {@link Trigger}，最後以
     * {@code replace = true} 方式排程，確保同名 Job 不會重複建立。</p>
     *
     * @param job             排程資料模型，含 Job 名稱、群組、類別等資訊
     * @param scheduleBuilder 排程策略建構器（CRON 或固定間隔）
     * @throws Exception 若 Job 類別不存在或排程器發生不可恢復的錯誤
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

        QuartzJobUtil quartzManageUtil = new QuartzJobUtil(job, propertyConfig.effectiveAllowedClasses());
        JobDetail jobDetail = null;
        Trigger newTrigger = null;
        try {
            jobDetail = quartzManageUtil.buildJobDetail(job);
            newTrigger = quartzManageUtil.buildJobTrigger(scheduleBuilder);
        } catch (ClassNotFoundException e) {
            log.error("Job name:{}, class not found, class name:{}", job.getName(), job.getClazz());
            // FIXME(待修 Bug)：buildJobDetail 失敗後 jobDetail / newTrigger 仍為 null，方法卻繼續往下執行，
            //   最終以 null 呼叫 scheduler.scheduleJob(null, set, true) 觸發 NullPointerException，
            //   被外層 catch 包成「未知錯誤」掩蓋真因。應在此 early return 或重新拋出例外。
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
     * 執行一次性 Job。
     *
     * <p>在 Job 名稱後附加 {@code -Once} 後綴，再呼叫 {@link #createJob} 以避免
     * 與相同名稱的常駐排程產生衝突。</p>
     *
     * @param job             排程資料模型
     * @param scheduleBuilder 排程策略建構器（通常為 {@code NOW} 立即執行模式）
     * @throws Exception 若底層 {@link #createJob} 發生例外時向上傳遞
     */
    private void executeOnce(Job job, ScheduleBuilder scheduleBuilder) throws Exception {
        job.setName(job.getName() + "-Once");
        createJob(job, scheduleBuilder);
    }

    /**
     * 將 {@link ScheduleJobVO} 轉換為 {@link Job} 資料模型。
     *
     * <p>VO 用於 REST 層的資料傳遞，{@link Job} 則供 {@link QuartzJobUtil}
     * 建立 {@link JobDetail} 與 {@link Trigger} 使用，
     * 此方法負責橋接兩者之間的欄位映射。</p>
     *
     * @param scheduleJobDetail 排程 VO，含前端或呼叫端傳入的排程參數
     * @return 轉換後的 {@link Job} 資料模型
     */
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
