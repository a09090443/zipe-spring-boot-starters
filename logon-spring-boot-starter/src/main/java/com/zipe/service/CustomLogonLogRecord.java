package com.zipe.service;

/**
 * 自訂登入日誌記錄介面。
 *
 * <p>業務系統可實作此介面，將登入成功、登入失敗、登出成功等事件
 * 寫入自訂的日誌儲存（例如資料庫稽核表或外部日誌系統）。
 * 框架在對應的登入／登出處理器中會自動呼叫所注入的實作。
 *
 * @author Gary Tsai
 */
public interface CustomLogonLogRecord {

    /**
     * 記錄使用者登入成功的日誌。
     *
     * @param userId 已成功登入的使用者識別碼
     */
    void recordLoginSuccessLog(String userId);

    /**
     * 記錄使用者登入失敗的日誌。
     *
     * @param userId 嘗試登入但失敗的使用者識別碼
     */
    void recordFailureLog(String userId);

    /**
     * 記錄使用者登出成功的日誌。
     *
     * @param userId 已成功登出的使用者識別碼
     */
    void recordLogoutSuccessLog(String userId);
}
