package com.example.config

import com.zipe.service.CustomLogonLogRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 自訂系統登出入紀錄
 *
 * @author Gary Tsai
 */
@Component
class LogonLogRecord : CustomLogonLogRecord {

    /**
     * 記錄使用者登入成功的日誌。
     *
     * @param userId 登入成功的使用者識別碼
     */
    override fun recordLoginSuccessLog(userId: String) {
        log.info("測試登入紀錄:{}", userId)
    }

    /**
     * 記錄使用者登入失敗的日誌。
     *
     * @param userId 登入失敗的使用者識別碼
     */
    override fun recordFailureLog(userId: String) {
        log.info("測試登入錯誤紀錄:{}", userId)
    }

    /**
     * 記錄使用者登出成功的日誌。
     *
     * @param userId 登出成功的使用者識別碼
     */
    override fun recordLogoutSuccessLog(userId: String) {
        log.info("測試登出紀錄:{}", userId)
    }

    companion object {
        private val log = LoggerFactory.getLogger(LogonLogRecord::class.java)
    }
}
