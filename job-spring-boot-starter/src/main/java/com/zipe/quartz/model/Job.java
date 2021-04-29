package com.zipe.quartz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.JobDataMap;

import java.time.LocalDateTime;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/28 下午 03:51
 **/
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
