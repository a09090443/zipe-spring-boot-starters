package com.zipe.quartz.job;

import com.zipe.quartz.config.QuartzJobPropertyConfig;
import com.zipe.quartz.enums.ScheduleEnum;
import com.zipe.quartz.model.Job;
import com.zipe.quartz.util.QuartzManageUtil;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class JobInit implements ApplicationRunner {

    private final Scheduler scheduler;

    private final QuartzJobPropertyConfig quartzJobPropertyConfig;

    @Autowired
    public JobInit(Scheduler scheduler,
                   QuartzJobPropertyConfig quartzJobPropertyConfig){
        this.scheduler = scheduler;
        this.quartzJobPropertyConfig = quartzJobPropertyConfig;
    }

    private static final String ID = "SUMMERDAY";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println(quartzJobPropertyConfig);
        Job job = new Job();
        job.setName("TestJob");
        job.setClazz("com.zipe.quartz.job.SecondJob");
        job.setGroup("TestSchedule");
        job.setCronExpression("0/10 * * * * ? *");
        QuartzManageUtil quartzManageUtil = new QuartzManageUtil(job);
        JobDetail detail = quartzManageUtil.buildJobDetail();
        Trigger trigger = quartzManageUtil.buildJobTrigger(ScheduleEnum.CRON.setExpression(job.getCronExpression()));
//        JobDetail jobDetail = JobBuilder.newJob(SecondJob.class)
//                .withIdentity(ID + " 02")
//                .withDescription("test")
//                .storeDurably()
//                .build();
//        ScheduleBuilder scheduleBuilder =
//                CronScheduleBuilder.cronSchedule("0/10 * * * * ? *");
//        // 建立任務觸發器
//        Trigger trigger = TriggerBuilder.newTrigger()
//                .forJob(jobDetail)
//                .withIdentity(ID + " 02Trigger")
//                .withSchedule(scheduleBuilder)
//                .startNow() //立即執行一次任務
//                .build();
        Set<Trigger> set = new HashSet<>();
        set.add(trigger);
        // boolean replace 表示啟動時對資料庫中的quartz的任務進行覆蓋。
        scheduler.scheduleJob(detail, set, true);
    }
}
