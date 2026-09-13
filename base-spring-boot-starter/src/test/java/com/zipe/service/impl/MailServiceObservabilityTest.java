package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static com.zipe.service.impl.MailFailoverTestSupport.unreachableServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.util.crypto.Base64Util;
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

        assertThatThrownBy(() -> {
                    service.setInitData();
                    service.simpleMailSend(plainTextMail("SC-16"));
                })
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(WRONG_PASSWORD_PLAIN));

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

        assertThatThrownBy(() -> {
                    service.setInitData();
                    service.simpleMailSend(plainTextMail("SC-17"));
                })
                .satisfies(e -> {
                    assertThat(e.getMessage()).doesNotContain(WRONG_PASSWORD_ENCODED_SOURCE);
                    assertThat(e.getMessage()).doesNotContain(encodedWrongPassword);
                });

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
}
