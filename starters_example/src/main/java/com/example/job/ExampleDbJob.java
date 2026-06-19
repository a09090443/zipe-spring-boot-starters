package com.example.job;

import com.zipe.quartz.job.QuartzJobFactory;
import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

import java.util.Map;

/**
 * 資料庫模式排程範例。
 *
 * <p>透過 REST API 動態將排程任務註冊至 Quartz，並將排程設定與參數
 * 持久化於資料庫中。本類別繼承 {@link QuartzJobFactory}，
 * 在 Quartz 觸發時由框架反射建立實例並呼叫 {@link #executeJob}。
 *
 * <p>適用情境：需要在執行期間動態新增、暫停或刪除排程，且希望重啟後
 * 仍能從資料庫還原排程狀態的場景。
 */
@Slf4j
public class ExampleDbJob extends QuartzJobFactory {

    /**
     * 排程觸發時的執行邏輯。
     *
     * <p>從 {@link org.quartz.JobDataMap} 中取得呼叫端傳入的自訂參數，
     * 並逐一印出以示範參數傳遞機制。
     *
     * @param jobExecutionContext Quartz 提供的執行上下文，包含 JobDetail、Trigger 等資訊
     */
    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) {
        // 從 JobDetail 取得呼叫端動態注入的參數 Map
        Map<String, Object> jobMap = jobExecutionContext.getJobDetail().getJobDataMap();
        log.info("{}執行, 當前的時間:{}", this.getClass(), DateTimeUtils.getDateNow());
        // 逐一印出所有參數鍵值，方便驗證排程參數是否正確傳入
        jobMap.forEach(
                (k, v) -> log.info("參數名稱{}, 參數內容{}", k, v)
        );
    }
}
