package com.zipe.quartz.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zipe.quartz.model.Job;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 驗證有效白名單 = 顯式 allowedJobClasses ∪ jobMap 中靜態設定的類別。
 */
class QuartzJobPropertyConfigTest {

    @Test
    void effectiveAllowListMergesExplicitAndStaticJobClasses() {
        QuartzJobPropertyConfig config = new QuartzJobPropertyConfig();
        config.setAllowedJobClasses(Set.of("com.example.ExplicitJob"));

        Map<String, Job> jobMap = new HashMap<>();
        Job job = new Job();
        job.setClazz("com.example.StaticJob");
        jobMap.put("static", job);
        config.setJobMap(jobMap);

        Set<String> effective = config.effectiveAllowedClasses();

        assertEquals(2, effective.size());
        assertTrue(effective.contains("com.example.ExplicitJob"));
        assertTrue(effective.contains("com.example.StaticJob"));
    }

    @Test
    void effectiveAllowListIsEmptyWhenNothingConfigured() {
        QuartzJobPropertyConfig config = new QuartzJobPropertyConfig();

        assertTrue(config.effectiveAllowedClasses().isEmpty());
    }
}
