package com.zipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.zipe.model.Mail;
import jakarta.mail.MessagingException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 編譯期驗證 {@link MailService} 既有五個方法的簽章（含 throws 宣告）未變更，
 * 對應情境測試計畫 SC-15、需求 REQ-MAIL-FAILOVER-008。
 *
 * <p>本測試以既有呼叫端慣用寫法（未因本次多 SMTP 容錯變更而修改任何一行）呼叫五個方法：
 * 若介面簽章有 breaking change（參數、回傳型別或 throws 宣告變動），本檔將無法通過編譯，
 * 使 {@code mvn -B verify} 直接失敗，達成「既有呼叫端程式碼無須修改即可編譯通過」的驗收。</p>
 */
class MailServiceSignatureCompatibilityTest {

    /**
     * 既有呼叫端慣用寫法：sendEmail 無 throws 宣告；simpleMailSend 無 throws 宣告；
     * attachedSend / richContentSend 宣告 throws MessagingException；
     * sendBatchMailWithFile 宣告 throws Exception。任何一項簽章改變都會導致本方法編譯失敗。
     */
    private void legacyStyleCaller(MailService mailService, Mail mail) throws Exception {
        mailService.setInitData();
        mailService.sendEmail(mail);
        mailService.simpleMailSend(mail);
        try {
            mailService.attachedSend(mail);
            mailService.richContentSend(mail);
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
        mailService.sendBatchMailWithFile(mail);
    }

    @Test
    void legacyCallerCompilesAgainstUnchangedInterface() {
        MailService noopMailService = new MailService() {
            @Override
            public void setInitData() {}

            @Override
            public void sendEmail(Mail mail) {}

            @Override
            public void simpleMailSend(Mail mail) {}

            @Override
            public void attachedSend(Mail mail) {}

            @Override
            public void richContentSend(Mail mail) {}

            @Override
            public void sendBatchMailWithFile(Mail mail) {}
        };

        Mail mail = new Mail();
        mail.setMailTo(new String[] {"user@example.com"});
        mail.setMailSubject("compat");
        mail.setMailContent("compat-body");

        assertThatCode(() -> legacyStyleCaller(noopMailService, mail)).doesNotThrowAnyException();
    }

    /**
     * SC-036：以反射逐一核對 {@link MailService} 六個既有方法（{@code setInitData} 與五個發送方法）
     * 的參數型別與 {@code throws} 宣告，而非僅靠編譯期呼叫（可能因參數恰好相容而遺漏 throws 宣告
     * 放寬／收窄等不會導致編譯失敗、但仍屬 breaking change 的簽章變動，例如新增 unchecked 例外
     * 對既有呼叫端而言不是編譯錯誤，但仍是行為契約變化）。
     */
    @Test
    void sixExistingMethods_haveUnchangedParameterTypesAndThrowsDeclarations() throws Exception {
        assertMethodSignature("setInitData", List.of(), List.of(MessagingException.class));
        assertMethodSignature("sendEmail", List.of(Mail.class), List.of());
        assertMethodSignature("simpleMailSend", List.of(Mail.class), List.of());
        assertMethodSignature("attachedSend", List.of(Mail.class), List.of(MessagingException.class));
        assertMethodSignature("richContentSend", List.of(Mail.class), List.of(MessagingException.class));
        assertMethodSignature("sendBatchMailWithFile", List.of(Mail.class), List.of(Exception.class));
    }

    private static void assertMethodSignature(
            String methodName, List<Class<?>> expectedParamTypes, List<Class<?>> expectedThrows) throws Exception {
        Method method = MailService.class.getMethod(methodName, expectedParamTypes.toArray(new Class<?>[0]));
        assertThat(method.getParameterTypes())
                .as("方法 %s 的參數型別", methodName)
                .containsExactlyElementsOf(expectedParamTypes);
        assertThat(method.getExceptionTypes())
                .as("方法 %s 的 throws 宣告", methodName)
                .containsExactlyInAnyOrderElementsOf(expectedThrows);
    }
}
