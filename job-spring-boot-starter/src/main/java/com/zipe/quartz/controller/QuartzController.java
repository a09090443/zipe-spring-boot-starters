package com.zipe.quartz.controller;

import org.quartz.Scheduler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.zipe.quartz.base.BaseJob;
import com.zipe.quartz.vo.ScheduleJobVO;

public class QuartzController extends BaseJob {

    public QuartzController(Scheduler scheduler) {
        super(scheduler);
    }

    /**
     * 註冊排程
     *
     * @param scheduleJobVO
     * @return
     */
    @PostMapping(path = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScheduleJobVO> registerJob(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.mergeJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 刪除排程
     *
     * @param scheduleJobVO
     * @return
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ScheduleJobVO> delete(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.deleteJobProcess(scheduleJobVO);

        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 暫停排程
     *
     * @param scheduleJobVO
     * @return
     */
    @PostMapping("/pause")
    public ResponseEntity<ScheduleJobVO> pause(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.pauseJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 回復排程
     *
     * @param scheduleJobVO
     * @return
     */
    @PostMapping("/resume")
    public ResponseEntity<ScheduleJobVO> resume(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.resumeJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 執行一次性排程
     *
     * @param scheduleJobVO
     * @return
     */
    @PostMapping("/run")
    public ResponseEntity<ScheduleJobVO> run(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.runJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

}
