package com.zipe.quartz.vo;

import lombok.Data;
import org.quartz.JobDataMap;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScheduleJobVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 排程名稱
     */
    private String jobName;
    /**
     * 排程說明
     */
    private String jobDescription;
    /**
     * 開始時間、首次執行時間
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastFireTime;
    /**
     * 下次執行時間
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextFireTime;
    /**
     * 排程時間單位
     */
    private Integer timeUnit;
    /**
     * 執行狀態
     */
    private String exeStatus;
    /**
     * 使用狀態
     */
    private Integer status;
    /**
     * 工作群組
     */
    private String jobGroup;
    /**
     * 工作所執行的程式名稱
     */
    private String jobClass;
    /**
     * 重複執行的時間區間
     */
    private Integer repeatInterval;
    /**
     * 執行次數
     */
    private Integer executeTimes;
    /**
     * Cron job 表示式
     */
    private String cronExpression;
    /**
     * 備註
     */
    private String message;
    /**
     * 排程開始時間
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    /**
     * 排程結束時間
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    /**
     * 排程參數
     */
    private JobDataMap jobDataMap;

}
