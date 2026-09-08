package com.zipe.config;

import lombok.Data;

/**
 * 多 SMTP 容錯切換行為設定，對應 {@code mail.failover.*} 屬性。
 *
 * <p>用於限制單次發送呼叫的最大嘗試組數與整體切換耗時上限，避免全部伺服器皆不可達時
 * 無上限累加阻塞呼叫端。</p>
 */
@Data
public class MailFailoverProperty {
    /**
     * 單次發送呼叫最多嘗試的 SMTP 伺服器組數上限；實際上限為此值與已設定伺服器組數的較小者。
     * Default=Integer.MAX_VALUE（即不額外限制，最多嘗試全部已設定的伺服器組）
     */
    private Integer maxAttempts = Integer.MAX_VALUE;
    /**
     * 單次發送呼叫允許的整體切換時間上限（毫秒）；逾時後不再嘗試下一組伺服器，直接以彙整失敗結束。
     * 設為 0 或負值表示不限制整體耗時（各組仍受各自的連線／讀取／寫入逾時限制）。
     * Default=30000（30 秒）
     */
    private Long overallTimeout = 30000L;
}
