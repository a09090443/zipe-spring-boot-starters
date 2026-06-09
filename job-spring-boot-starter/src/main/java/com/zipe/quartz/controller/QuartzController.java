package com.zipe.quartz.controller;

import com.zipe.quartz.base.BaseJob;
import com.zipe.quartz.config.QuartzJobPropertyConfig;
import com.zipe.quartz.vo.ScheduleJobVO;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 排程管理 REST API。
 *
 * <p><b>資安提醒：</b>此控制器可動態建立 / 刪除排程，預設為關閉狀態，需明確設定
 * {@code quartz.controller.enabled=true} 才會註冊。啟用後務必納入 Spring Security
 * 等存取控制保護，且僅能載入白名單（{@code quartz.allowed-job-classes}）內的 Job 類別。</p>
 *
 * @author : Gary Tsai
 **/
@RestController
@RequestMapping("/quartz")
@ConditionalOnProperty(name = "quartz.controller.enabled", havingValue = "true")
public class QuartzController extends BaseJob {

    /**
     * 建構 QuartzController，注入 Quartz Scheduler 與排程屬性設定。
     *
     * @param scheduler      Quartz 排程器實體，由 Spring 容器自動注入
     * @param propertyConfig 排程相關屬性設定（允許的 Job 類別白名單等）
     */
    public QuartzController(Scheduler scheduler, QuartzJobPropertyConfig propertyConfig) {
        super(scheduler, propertyConfig);
    }

    /**
     * 註冊或更新排程。
     *
     * <p>若同名的 Job 尚不存在則新增，已存在則更新其 Cron 表達式與參數。</p>
     *
     * @param scheduleJobVO 排程設定資料，包含 Job 名稱、群組、類別與 Cron 表達式
     * @return 處理後的 {@link ScheduleJobVO}，HTTP 200 包裝於 {@link ResponseEntity}
     */
    @PostMapping(path = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScheduleJobVO> registerJob(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.mergeJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 刪除指定排程。
     *
     * <p>從 Quartz Scheduler 中移除對應的 Trigger 與 JobDetail。</p>
     *
     * @param scheduleJobVO 待刪除排程的識別資料（Job 名稱與群組）
     * @return 處理後的 {@link ScheduleJobVO}，HTTP 200 包裝於 {@link ResponseEntity}
     */
    @PostMapping("/delete")
    public ResponseEntity<ScheduleJobVO> delete(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.deleteJobProcess(scheduleJobVO);

        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 暫停指定排程。
     *
     * <p>排程暫停後不再觸發，可透過 {@code /resume} 恢復執行。</p>
     *
     * @param scheduleJobVO 待暫停排程的識別資料（Job 名稱與群組）
     * @return 處理後的 {@link ScheduleJobVO}，HTTP 200 包裝於 {@link ResponseEntity}
     */
    @PostMapping("/pause")
    public ResponseEntity<ScheduleJobVO> pause(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.pauseJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 恢復已暫停的排程。
     *
     * <p>將排程從暫停狀態恢復為正常排程，繼續依 Cron 表達式觸發。</p>
     *
     * @param scheduleJobVO 待恢復排程的識別資料（Job 名稱與群組）
     * @return 處理後的 {@link ScheduleJobVO}，HTTP 200 包裝於 {@link ResponseEntity}
     */
    @PostMapping("/resume")
    public ResponseEntity<ScheduleJobVO> resume(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.resumeJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 立即觸發一次性排程執行。
     *
     * <p>不影響原有的 Cron 設定；Job 會在當下立即執行一次，執行完畢後仍依原排程繼續。</p>
     *
     * @param scheduleJobVO 待立即執行排程的識別資料（Job 名稱與群組）
     * @return 處理後的 {@link ScheduleJobVO}，HTTP 200 包裝於 {@link ResponseEntity}
     */
    @PostMapping("/run")
    public ResponseEntity<ScheduleJobVO> run(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.runJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

}
