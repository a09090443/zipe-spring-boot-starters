package com.zipe.quartz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.JobDataMap;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * @author : Gary Tsai
 **/
@Configuration
@ConfigurationProperties(prefix = "quartz.job")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    /**
     * 排程名稱
     */
    private String name;
    /**
     * 排程描述
     */
    private String description;
    /**
     * 排程組名稱
     */
    private String group;
    /**
     * 排程對應的 class
     */
    private String clazz;
    /**
     * 排程執行時間表示法
     * Sample:0/30 * * * * ? *
     */
    private String cronExpression;
    /**
     * 排程開始時間
     */
    private LocalDateTime startTime ;
    /**
     * 排程結束時間
     */
    private LocalDateTime endTime ;
    /**
     * 排程執行時所帶入參數內容
     */
    private JobDataMap dataMap = new JobDataMap();
}
