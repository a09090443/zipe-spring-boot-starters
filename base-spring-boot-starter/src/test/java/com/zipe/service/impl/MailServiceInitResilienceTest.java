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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * {@code MailService.setInitData()} 初始化韌性情境測試，對應情境測試計畫 SC-10、SC-11。
 *
 * <p>對應 REQ-MAIL-FAILOVER-005：初始化階段不得因部分 SMTP 不可用而使服務或應用程式啟動失敗，
 * 只要至少一組可用即應可提供服務；全部組別皆不可用時，失敗行為須與 REQ-003 一致且訊息可辨識。</p>
 */
class MailServiceInitResilienceTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("backupUser", "backupPw"));

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    /** SC-10：第一組指向不存在主機、第二組正常時，setInitData 不拋出例外，且有 WARN 記錄，後續發送仍可由第二組送達。 */
    @Test
    void setInitData_partialFailure_doesNotThrow_andLogsWarning_andCanStillSend() throws Exception {
        MailPropertyConfig cfg = config(
                unreachableServer("primary"),
                greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        MailServiceImpl service = new MailServiceImpl(cfg);

        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        assertThatCode(service::setInitData).doesNotThrowAnyException();

        boolean hasWarnForPrimary = appender.list.stream()
                .anyMatch(event -> event.getLevel().toString().equals("WARN")
                        && event.getFormattedMessage().contains("primary"));
        assertThat(hasWarnForPrimary).isTrue();

        service.simpleMailSend(plainTextMail("SC-10"));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-10");
    }

    /** SC-11：初始化階段全部 SMTP 皆不可用時，拋出 MessagingException，訊息可辨識兩組皆已嘗試與各自失敗原因。 */
    @Test
    void setInitData_allServersDown_throwsMessagingExceptionWithIdentifiableReasons() {
        MailPropertyConfig cfg = config(unreachableServer("primary", 1), unreachableServer("secondary", 2));
        MailServiceImpl service = new MailServiceImpl(cfg);

        assertThatThrownBy(service::setInitData)
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("primary")
                .hasMessageContaining("secondary");
    }
}
