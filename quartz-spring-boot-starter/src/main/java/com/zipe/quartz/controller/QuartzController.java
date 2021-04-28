package com.zipe.quartz.controller;

import com.zipe.quartz.base.BaseJob;
import com.zipe.quartz.vo.ScheduleJobVO;
import org.quartz.Scheduler;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/28 上午 09:36
 **/
@RestController
@RequestMapping("/quartz")
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
    public String registerJob(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.mergeJobProcess(scheduleJobVO);
        return "";
    }
}
