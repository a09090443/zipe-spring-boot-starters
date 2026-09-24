package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static com.zipe.service.impl.MailFailoverTestSupport.unreachableServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.exception.MailFailoverException;
import com.zipe.util.crypto.Base64Util;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * 多 SMTP 容錯切換可觀測性與敏感資訊防護情境測試，對應情境測試計畫 SC-12、SC-16、SC-17、SC-27。
 *
 * <p>對應 REQ-MAIL-FAILOVER-006（切換須可觀測）與 REQ-MAIL-FAILOVER-009（帳密不得出現於日誌／例外訊息）。</p>
 */
class MailServiceObservabilityTest {

    private static final String WRONG_PASSWORD_PLAIN = "WRONG_SECRET_998";
    private static final String WRONG_PASSWORD_ENCODED_SOURCE = "WRONG_SECRET_999";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("authUser", "correctPw"));

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    /** SC-12：每次切換與最終成功組別皆有可觀測日誌，含來源組別識別與失敗原因摘要、最終成功組別。 */
    @Test
    void switchingBetweenServers_producesObservableLogs() throws Exception {
        MailPropertyConfig cfg = config(
                unreachableServer("primary"),
                greenMailServer("backup", greenMail.getSmtp().getPort(), "authUser", "correctPw"));
        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        service.simpleMailSend(plainTextMail("SC-12"));

        List<String> messages =
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());

        assertThat(messages).anyMatch(m -> m.contains("primary") && (m.contains("失敗") || m.contains("將嘗試下一組")));
        assertThat(messages).anyMatch(m -> m.contains("backup") && m.contains("成功"));
    }

    /**
     * SC-002：有設定 name 的伺服器組以 name 標示，未設定 name 的伺服器組精確退回
     * {@code host:port} 標示，兩者於同一次失敗切換中須可同時分辨（正面與 counterexample 併存）。
     */
    @Test
    void unnamedServer_fallsBackToHostPortLabel_whileNamedServerUsesName() throws Exception {
        MailPropertyConfig cfg = config(unreachableServer("named-primary", 1), unreachableServer(null, 2));
        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (jakarta.mail.MessagingException ignored) {
            // 兩組皆不可用，仍保留候選清單供後續發送嘗試。
        }

        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        Throwable failure = catchThrowable(() -> service.simpleMailSend(plainTextMail("SC-002")));

        assertThat(failure).isInstanceOf(MailFailoverException.class);
        assertThat(failure.getMessage()).contains("named-primary").contains("127.0.0.1:2");

        String allLogText = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        assertThat(allLogText).contains("named-primary").contains("127.0.0.1:2");
    }

    /** SC-16：認證失敗情境下，全部日誌與例外訊息皆不含明文密碼字串，僅含 host、port 與錯誤類型。 */
    @Test
    void authenticationFailure_neverLeaksPlainPassword() {
        MailServerProperty badAuthServer = new MailServerProperty();
        badAuthServer.setName("primary");
        badAuthServer.setHost("127.0.0.1");
        badAuthServer.setPort(String.valueOf(greenMail.getSmtp().getPort()));
        badAuthServer.setUsername("authUser");
        badAuthServer.setPa55word(WRONG_PASSWORD_PLAIN);
        badAuthServer.setSmtpAuthEnable(true);

        MailPropertyConfig cfg = config(badAuthServer);
        MailServiceImpl service = new MailServiceImpl(cfg);

        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        Throwable initFailure = catchThrowable(service::setInitData);
        Throwable sendFailure = catchThrowable(() -> service.simpleMailSend(plainTextMail("SC-16")));

        assertThat(initFailure).isInstanceOf(jakarta.mail.MessagingException.class);
        assertThat(sendFailure).isInstanceOf(MailFailoverException.class);
        assertThat(sendFailure.getSuppressed()).hasSize(1);
        assertThat(exceptionText(initFailure) + exceptionText(sendFailure))
                .doesNotContain(WRONG_PASSWORD_PLAIN);

        String allLogText = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        assertThat(allLogText).doesNotContain(WRONG_PASSWORD_PLAIN);
    }

    /** SC-17：encrypt-enable=true（Base64 編碼）情境下，日誌與例外訊息不含編碼前明文密碼，亦不含 Base64 編碼後字串。 */
    @Test
    void authenticationFailure_withEncryptEnable_neverLeaksEncodedPassword() {
        String encodedWrongPassword = new Base64Util().getEncrypt(WRONG_PASSWORD_ENCODED_SOURCE);

        MailServerProperty badAuthServer = new MailServerProperty();
        badAuthServer.setName("primary");
        badAuthServer.setHost("127.0.0.1");
        badAuthServer.setPort(String.valueOf(greenMail.getSmtp().getPort()));
        badAuthServer.setUsername("authUser");
        badAuthServer.setPa55word(encodedWrongPassword);
        badAuthServer.setEncryptEnable(true);
        badAuthServer.setSmtpAuthEnable(true);

        MailPropertyConfig cfg = config(badAuthServer);
        MailServiceImpl service = new MailServiceImpl(cfg);

        ListAppender<ILoggingEvent> mailAppender =
                MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);
        ListAppender<ILoggingEvent> base64Appender =
                MailFailoverTestSupport.attachLogAppender(Base64Util.class);

        Throwable initFailure = catchThrowable(service::setInitData);
        Throwable sendFailure = catchThrowable(() -> service.simpleMailSend(plainTextMail("SC-17")));

        assertThat(initFailure).isInstanceOf(jakarta.mail.MessagingException.class);
        assertThat(sendFailure).isInstanceOf(MailFailoverException.class);
        assertThat(sendFailure.getSuppressed()).hasSize(1);
        assertThat(exceptionText(initFailure) + exceptionText(sendFailure))
                .doesNotContain(WRONG_PASSWORD_ENCODED_SOURCE)
                .doesNotContain(encodedWrongPassword);

        String allLogText = List.of(mailAppender, base64Appender).stream()
                .flatMap(appender -> appender.list.stream())
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        assertThat(allLogText).doesNotContain(WRONG_PASSWORD_ENCODED_SOURCE);
        assertThat(allLogText).doesNotContain(encodedWrongPassword);
    }

    /** SC-27：sendEmail 於全部 SMTP 皆失敗時仍不對外拋出例外，但須有可觀測之失敗日誌，且不得誤植為成功訊息。 */
    @Test
    void sendEmail_allServersDown_doesNotThrow_butLogsFailureNotSuccess() throws Exception {
        MailPropertyConfig cfg = config(unreachableServer("primary", 1), unreachableServer("secondary", 2));
        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (jakarta.mail.MessagingException ignored) {
            // 兩組皆不可用，setInitData 依 SC-11 語意拋出，候選清單仍保留供後續嘗試
        }

        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        assertThatCode(() -> service.sendEmail(MailFailoverTestSupport.htmlMail("SC-27")))
                .doesNotThrowAnyException();

        List<String> messages =
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
        assertThat(messages).anyMatch(m -> m.contains("最終失敗"));
        assertThat(messages).noneMatch(m -> m.contains("成功"));
    }

    /**
     * SC-034：前兩組失敗、第三組成功時，日誌須能還原完整的嘗試順序——依序看到 primary、secondary
     * 兩筆 WARN 失敗記錄（且順序與候選清單一致），最後才是 tertiary 的成功 INFO 記錄；
     * 不得只留下最終成功訊息而遺漏中途的失敗記錄，也不得順序錯亂。
     */
    @Test
    void twoFailuresThenSuccess_logsPreserveFullAttemptOrder() throws Exception {
        MailPropertyConfig cfg = config(
                unreachableServer("primary", 1),
                unreachableServer("secondary", 2),
                greenMailServer("tertiary", greenMail.getSmtp().getPort(), "authUser", "correctPw"));
        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (jakarta.mail.MessagingException ignored) {
            // primary/secondary 於初始化階段即失敗，仍保留候選清單供後續發送嘗試
        }

        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        service.simpleMailSend(MailFailoverTestSupport.plainTextMail("SC-034"));

        List<String> messages =
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());

        int primaryWarnIndex = indexOfFirstMatch(messages, m -> m.contains("primary") && m.contains("將嘗試下一組"));
        int secondaryWarnIndex = indexOfFirstMatch(messages, m -> m.contains("secondary") && m.contains("將嘗試下一組"));
        int tertiarySuccessIndex = indexOfFirstMatch(messages, m -> m.contains("tertiary") && m.contains("成功"));

        assertThat(primaryWarnIndex).as("primary 的 WARN 失敗記錄須存在").isNotNegative();
        assertThat(secondaryWarnIndex).as("secondary 的 WARN 失敗記錄須存在").isNotNegative();
        assertThat(tertiarySuccessIndex).as("tertiary 的成功 INFO 記錄須存在").isNotNegative();
        assertThat(primaryWarnIndex).as("primary 須早於 secondary 被記錄").isLessThan(secondaryWarnIndex);
        assertThat(secondaryWarnIndex).as("secondary 須早於 tertiary 成功被記錄").isLessThan(tertiarySuccessIndex);
    }

    /**
     * SC-035：max-attempts 小於已設定組數、且被嘗試的組別全數失敗時，ERROR 彙整日誌須顯示
     * 「實際嘗試組數」（等於 max-attempts），而非誤植為候選清單的總組數；未被嘗試的多餘組別
     * 也不得出現在彙整清單中，避免維運誤判實際嘗試了多少組。
     */
    @Test
    void allAttemptedServersDown_errorLogShowsActualAttemptedCount_notTotalCandidateCount() throws Exception {
        MailPropertyConfig cfg = config(
                unreachableServer("primary", 1), unreachableServer("secondary", 2), unreachableServer("tertiary", 3));
        cfg.getFailover().setMaxAttempts(2);
        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (jakarta.mail.MessagingException ignored) {
            // 三組皆於初始化階段失敗，仍保留候選清單供後續發送嘗試
        }

        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        Throwable failure =
                catchThrowable(() -> service.simpleMailSend(MailFailoverTestSupport.plainTextMail("SC-035")));
        assertThat(failure).isInstanceOf(MailFailoverException.class);

        List<String> errorMessages = appender.list.stream()
                .filter(event -> event.getLevel().toString().equals("ERROR"))
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());

        assertThat(errorMessages).as("須有一筆最終失敗的 ERROR 日誌").hasSize(1);
        String errorMessage = errorMessages.get(0);
        // 已嘗試組數須為 max-attempts（2），而非候選總數（3）
        assertThat(errorMessage).contains("已嘗試 2 組");
        assertThat(errorMessage).doesNotContain("已嘗試 3 組");
        assertThat(errorMessage).contains("primary").contains("secondary");
        assertThat(errorMessage).doesNotContain("tertiary");
    }

    private static int indexOfFirstMatch(List<String> messages, java.util.function.Predicate<String> predicate) {
        for (int i = 0; i < messages.size(); i++) {
            if (predicate.test(messages.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String exceptionText(Throwable failure) {
        StringWriter text = new StringWriter();
        failure.printStackTrace(new PrintWriter(text));
        return text.toString();
    }
}
