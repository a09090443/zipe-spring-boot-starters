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

    public QuartzController(Scheduler scheduler, QuartzJobPropertyConfig propertyConfig) {
        super(scheduler, propertyConfig);
    }

    /**
     * 註冊排程
     *
     * @param scheduleJobVO
     * @return
     */
    @PostMapping(path = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScheduleJobVO> registerJob(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.mergeJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 刪除排程
     *
     * @param scheduleJobVO
     * @return
     */
    @PostMapping("/delete")
    public ResponseEntity<ScheduleJobVO> delete(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.deleteJobProcess(scheduleJobVO);

        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 暫停排程
     *
     * @param scheduleJobVO
     * @return
     */
    @PostMapping("/pause")
    public ResponseEntity<ScheduleJobVO> pause(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.pauseJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 回復排程
     *
     * @param scheduleJobVO
     * @return
     */
    @PostMapping("/resume")
    public ResponseEntity<ScheduleJobVO> resume(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.resumeJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

    /**
     * 執行一次性排程
     *
     * @param scheduleJobVO
     * @return
     */
    @PostMapping("/run")
    public ResponseEntity<ScheduleJobVO> run(@RequestBody ScheduleJobVO scheduleJobVO) {
        this.runJobProcess(scheduleJobVO);
        return ResponseEntity.ok(scheduleJobVO);
    }

}
