package com.zipe.quartz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.JobDataMap;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    private String name;
    private String description;
    private String group;
    private String clazz;
    private String cronExpression;
    private LocalDateTime startTime ;
    private LocalDateTime endTime ;
    private JobDataMap dataMap = new JobDataMap();
}
