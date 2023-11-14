package com.example.controller;

import com.zipe.quartz.controller.QuartzController;
import org.quartz.Scheduler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job")
public class ScheduleController extends QuartzController {
    public ScheduleController(Scheduler scheduler) {
        super(scheduler);
    }
}
