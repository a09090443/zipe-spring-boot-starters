package com.zipe.quartz.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zipe.quartz.model.Job;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

/**
 * 驗證 QuartzJobUtil 僅允許白名單內、且實作 org.quartz.Job 的類別被載入，
 * 避免透過任意類別名稱載入造成 RCE。
 */
class QuartzJobUtilTest {

    /** 測試用合法 Job。 */
    public static class DummyJob implements org.quartz.Job {
        @Override
        public void execute(JobExecutionContext context) {
            // no-op
        }
    }

    private Job newJob(String clazz) {
        Job job = new Job();
        job.setName("test");
        job.setGroup("test");
        job.setClazz(clazz);
        return job;
    }

    @Test
    void buildsJobDetailWhenClassIsAllowed() throws Exception {
        String className = DummyJob.class.getName();
        Job job = newJob(className);
        QuartzJobUtil util = new QuartzJobUtil(job, Set.of(className));

        JobDetail detail = util.buildJobDetail(job);

        assertNotNull(detail);
        assertEquals(DummyJob.class, detail.getJobClass());
    }

    @Test
    void rejectsClassNotInAllowList() {
        String className = DummyJob.class.getName();
        Job job = newJob(className);
        QuartzJobUtil util = new QuartzJobUtil(job, Collections.emptySet());

        assertThrows(SecurityException.class, () -> util.buildJobDetail(job));
    }

    @Test
    void rejectsClassThatIsNotAQuartzJob() {
        // String 在白名單內，但不是 org.quartz.Job，仍須拒絕
        String className = "java.lang.String";
        Job job = newJob(className);
        QuartzJobUtil util = new QuartzJobUtil(job, Set.of(className));

        assertThrows(SecurityException.class, () -> util.buildJobDetail(job));
    }
}
