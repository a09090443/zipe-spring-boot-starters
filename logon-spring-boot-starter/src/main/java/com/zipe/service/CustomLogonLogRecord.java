package com.zipe.service;

/**
 * @author Gary Tsai
 */
public interface CustomLogonLogRecord {
    void recordLoginSuccessLog(String userId);

    void recordFailureLog(String userId);

    void recordLogoutSuccessLog(String userId);
}
