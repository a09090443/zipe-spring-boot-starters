package com.zipe.quartz.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.JobDataMap;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 排程作業的資料模型。
 *
 * <p>透過 {@code @ConfigurationProperties(prefix = "quartz.job")} 與
 * {@code application.yml} / {@code application.properties} 中的
 * {@code quartz.job.*} 屬性綁定，持有單一排程的完整設定資訊，
 * 包含排程名稱、群組、對應的執行類別、Cron 表達式、起訖時間
 * 以及傳遞給 Job 的參數映射。
 *
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
