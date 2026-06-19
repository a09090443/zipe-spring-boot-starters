package com.zipe.base.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

import java.time.LocalDateTime;

/**
 * P6Spy SQL 日誌格式化器。
 *
 * <p>實作 {@link MessageFormattingStrategy} 介面，自訂 P6Spy 攔截 SQL 時輸出的日誌格式。
 * 每筆 SQL 執行後，將連線資訊、執行耗時、類別與完整 SQL 語句組合成易讀的單行日誌。
 * 若 SQL 內容為空（例如 P6Spy 內部事件），則回傳空字串以避免產生無意義的日誌。
 */
public class P6SpyLogger implements MessageFormattingStrategy {
    /**
     * 將 P6Spy 提供的 SQL 執行資訊格式化為自訂日誌字串。
     *
     * @param connectionId 資料庫連線識別碼
     * @param now          P6Spy 記錄的當前時間字串（由框架傳入，本實作改用 {@link LocalDateTime#now()} 取得更精確的時間）
     * @param elapsed      SQL 執行耗時（毫秒）
     * @param category     P6Spy 事件類別，例如 statement、commit 等
     * @param prepared     含有佔位符（?）的原始預備 SQL 語句
     * @param sql          已將佔位符替換為實際參數值的完整 SQL 語句
     * @param url          資料來源連線 URL
     * @return 格式化後的日誌字串；若 sql 內容為空白則回傳空字串
     */
    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        // 過濾空 SQL（P6Spy 部分內部事件不帶 SQL，直接略過避免產生空行日誌）
        return !"".equals(sql.trim()) ? "P6SpyLogger " + LocalDateTime.now() + " | elapsed " + elapsed + "ms | category " + category + " | connection " + connectionId + " | url " + url + " | sql \n" + sql : "";
    }
}
