package com.zipe.quartz.config;

import com.zipe.quartz.model.Job;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 排程屬性設定類別。
 * <p>
 * 對應 {@code application.yml} 中 {@code quartz} 前綴的設定區塊，
 * 負責持有靜態排程定義（{@code jobMap}）以及允許動態載入的 Job 類別白名單
 * （{@code allowedJobClasses}）。Spring Boot 啟動時會自動將設定值繫結至此類別。
 * </p>
 *
 * @author : Gary Tsai
 **/
@Configuration
@ConfigurationProperties(prefix = "quartz")
@Data
public class QuartzJobPropertyConfig {
    /**
     * 靜態排程定義映射表，Key 為排程識別名稱，Value 為對應的 {@link Job} 設定物件。
     * 對應設定格式為 {@code quartz.job-map.<name>.*}。
     */
    private Map<String, Job> jobMap;

    /**
     * 允許透過排程管理 API 載入的 Job 類別全名白名單。
     * 為防止任意類別載入造成 RCE，未列於此處（且非靜態 jobMap 設定）的類別一律拒絕。
     */
    private Set<String> allowedJobClasses = new HashSet<>();

    /**
     * 有效白名單 = 顯式設定的 {@code allowedJobClasses} 聯集 {@code jobMap} 中
     * 所有靜態設定的 Job 類別（靜態設定為信任來源，自動納入白名單）。
     *
     * @return 允許載入的 Job 類別全名集合
     */
    public Set<String> effectiveAllowedClasses() {
        Set<String> effective = new HashSet<>();
        if (allowedJobClasses != null) {
            effective.addAll(allowedJobClasses);
        }
        if (jobMap != null) {
            jobMap.values().stream()
                    .filter(job -> job != null && job.getClazz() != null)
                    .forEach(job -> effective.add(job.getClazz()));
        }
        return effective;
    }
}
