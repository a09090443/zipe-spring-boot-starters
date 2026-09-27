package com.zipe.exception;

import java.util.List;
import org.springframework.mail.MailException;

/**
 * 多 SMTP 容錯切換全部嘗試皆失敗時拋出的例外。
 *
 * <p>為 unchecked 例外（繼承 {@link MailException}），不需變更 {@code MailService}
 * 既有方法的 {@code throws} 宣告即可拋出，藉此在維持對外簽章不變的前提下，
 * 確保「所有 SMTP 皆失敗」不會被靜默視為成功。每一組的失敗原因以
 * {@link #addSuppressed(Throwable)} 掛載，可透過 {@link #getSuppressed()} 逐一檢視。</p>
 */
public class MailFailoverException extends MailException {

    public MailFailoverException(String operationName, List<String> failureSummaries) {
        super(buildMessage(operationName, failureSummaries));
    }

    private static String buildMessage(String operationName, List<String> failureSummaries) {
        return "郵件發送失敗（" + operationName + "）：已嘗試 " + failureSummaries.size()
                + " 組 SMTP 伺服器皆失敗 -> " + String.join("; ", failureSummaries);
    }
}
