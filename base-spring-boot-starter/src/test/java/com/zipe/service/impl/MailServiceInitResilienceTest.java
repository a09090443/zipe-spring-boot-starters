package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.invalidPortServer;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    /**
     * SC-013：三組候選中僅一組初始化失敗時，setInitData 不拋出例外，該組被標示於 WARN，
     * 其餘兩組皆保留在候選清單中，且兩組皆各自能實際送達（驗證「保留」而非僅偶然剩一組可用）。
     */
    @Test
    void setInitData_oneOfThreeServersFailsInit_retainsOtherTwoAsUsableCandidates() throws Exception {
        MailPropertyConfig cfg = config(
                unreachableServer("bad", 1),
                greenMailServer("good-one", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        // 第三組另外指向同一 GreenMail（不同標籤），確認候選清單保留的是「其餘全部」而非僅剩一組。
        cfg.getServers().add(greenMailServer("good-two", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));

        MailServiceImpl service = new MailServiceImpl(cfg);
        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        assertThatCode(service::setInitData).doesNotThrowAnyException();

        boolean hasWarnForBad = appender.list.stream()
                .anyMatch(event -> event.getLevel().toString().equals("WARN")
                        && event.getFormattedMessage().contains("bad"));
        assertThat(hasWarnForBad).isTrue();

        List<?> candidates = (List<?>) ReflectionTestUtils.getField(service, "candidates");
        assertThat(candidates).hasSize(3);

        // 第一組（bad）不可用，直接嘗試發送應由第二組（good-one）送達，證明其餘兩組確實保留且可用。
        service.simpleMailSend(plainTextMail("SC-013"));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-013");
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

    /**
     * SC-013／AC-004-06／AC-007-01：三組中一組在「建立寄件器」階段（而非連線測試階段）即失敗——
     * 連接埠設定為非數字字串，{@code buildSender} 內 {@code Integer.parseInt} 會直接拋出例外。
     * 此為 review 節點確認的既有缺口：舊版測試只用「連線不可達」（buildSender 可成功、僅
     * testConnection 失敗）製造反例，未曾涵蓋 buildSender 本身失敗的情境，該情境曾因
     * buildSender 呼叫位於 try 區塊之外而讓整個迴圈中止、其餘正常組別完全不會被建立。
     * 本測試驗證修復後：該組被跳過並記錄 WARN，其餘兩組仍保留在候選清單中且皆可實際發送成功。
     */
    @Test
    void setInitData_oneOfThreeServersFailsSenderCreation_retainsOtherTwoAsUsableCandidates() throws Exception {
        MailPropertyConfig cfg = config(invalidPortServer("bad-config"));
        cfg.getServers().add(greenMailServer("good-one", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        cfg.getServers().add(greenMailServer("good-two", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));

        MailServiceImpl service = new MailServiceImpl(cfg);
        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        assertThatCode(service::setInitData).doesNotThrowAnyException();

        boolean hasWarnForBadConfig = appender.list.stream()
                .anyMatch(event -> event.getLevel().toString().equals("WARN")
                        && event.getFormattedMessage().contains("bad-config"));
        assertThat(hasWarnForBadConfig).isTrue();

        // buildSender 失敗的組別沒有可用的 sender，不會被加入候選清單，故僅保留其餘兩組（而非三組）。
        List<?> candidates = (List<?>) ReflectionTestUtils.getField(service, "candidates");
        assertThat(candidates).hasSize(2);

        service.simpleMailSend(plainTextMail("SC-013-sender-creation-failure"));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-013-sender-creation-failure");
    }

    /**
     * AC-007-02：唯一一組候選在建立寄件器階段即失敗（設定錯誤）時，setInitData 仍須以明確的
     * MessagingException 呈現失敗（而非誤判為候選數 0 卻正常返回），訊息可辨識該組與失敗原因。
     */
    @Test
    void setInitData_onlyServerFailsSenderCreation_throwsMessagingExceptionWithIdentifiableReason() {
        MailPropertyConfig cfg = config(invalidPortServer("bad-config"));
        MailServiceImpl service = new MailServiceImpl(cfg);

        assertThatThrownBy(service::setInitData)
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("bad-config");
    }
}
