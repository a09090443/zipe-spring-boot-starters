package com.zipe.quartz.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.quartz.JobDataMap;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;
    /**
     * 排程結束時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
    /**
     * 排程參數
     */
    private JobDataMap jobDataMap = new JobDataMap();

}
