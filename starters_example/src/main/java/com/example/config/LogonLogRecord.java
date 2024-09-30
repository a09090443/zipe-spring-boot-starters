package com.example.config;

import com.zipe.service.CustomLogonLogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自訂系統登出入紀錄
 *
 * @author Gary Tsai
 */
@Slf4j
@Component
//@ComponentScan(basePackages = {"com.zipe"})
public class LogonLogRecord implements CustomLogonLogRecord {
    @Override
    public void recordLoginSuccessLog(String userId) {
        log.info("測試登入紀錄:{}", userId);
    }

    @Override
    public void recordFailureLog(String userId) {
        log.info("測試登入錯誤紀錄:{}", userId);
    }

    @Override
    public void recordLogoutSuccessLog(String userId) {
        log.info("測試登出紀錄:{}", userId);
    }
}
