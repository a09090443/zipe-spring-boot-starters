package com.zipe.quartz.job;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 驗證 {@link QuartzJobFactory#executeInternal} 的模板流程：
 * 前置→執行→後置，且子類別例外被吞掉並轉為錯誤日誌，不向上拋出。
 */
class QuartzJobFactoryTest {

    /**
     * 可記錄各階段是否被呼叫的測試用子類別，覆寫日誌方法以避免依賴 JobDetail。
     */
    private static class RecordingJob extends QuartzJobFactory {
        boolean before;
        boolean after;
        boolean error;
        boolean executed;
        final RuntimeException toThrow;

        RecordingJob(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        protected void executeJob(JobExecutionContext jobExecutionContext) {
            executed = true;
            if (toThrow != null) {
                throw toThrow;
            }
        }

        @Override
        protected void beforeExecuteJobLog(JobExecutionContext jobExecutionContext) {
            before = true;
        }

        @Override
        protected void afterExecuteJobLog(JobExecutionContext jobExecutionContext) {
            after = true;
        }

        @Override
        protected void errorExecuteJobLog(JobExecutionContext jobExecutionContext, Exception e) {
            error = true;
        }

        void run(JobExecutionContext context) throws JobExecutionException {
            executeInternal(context);
        }
    }

    /**
     * 正常執行：前置、執行、後置皆觸發，且不記錄錯誤。
     */
    @Test
    void executesBeforeJobAfterOnSuccess() throws JobExecutionException {
        RecordingJob job = new RecordingJob(null);
        // 測試子類別已覆寫所有日誌方法，不會解參考 context，故可安全傳入 null
        job.run(null);

        assertTrue(job.before);
        assertTrue(job.executed);
        assertTrue(job.after);
        assertFalse(job.error);
    }

    /**
     * 子類別拋例外：例外被吞掉（不向上拋出），記錄錯誤日誌，後置流程仍執行。
     */
    @Test
    void swallowsExceptionAndStillRunsAfterLog() {
        RecordingJob job = new RecordingJob(new RuntimeException("boom"));

        assertDoesNotThrow(() -> job.run(null));
        assertTrue(job.error);
        assertTrue(job.after);
    }
}
