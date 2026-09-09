package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * 僅設定舊版單組 mail.* 扁平屬性（未設定 mail.servers）之端到端送信情境測試，對應情境測試計畫 SC-14。
 *
 * <p>對應 REQ-MAIL-FAILOVER-008：既有以單組 mail.host/mail.port/mail.username/mail.pa55word
 * 設定的使用者升級後不需修改設定即可維持原有行為。既有的 {@code resolveServersFallsBackToFlatFieldsWhenServersEmpty}
 * 僅斷言物件欄位，本測試補上實際透過 GreenMail 送達的端到端驗證。</p>
 */
class MailServiceLegacyCompatibilityTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("legacyUser", "legacyPw"));

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    /** SC-14：僅設定舊版扁平 mail.* 屬性（未設定 mail.servers）時，初始化與發送皆與升級前行為一致。 */
    @Test
    void flatLegacyProperties_withoutServersList_stillInitializesAndSendsSuccessfully() throws Exception {
        MailPropertyConfig cfg = new MailPropertyConfig();
        cfg.setHost("127.0.0.1");
        cfg.setPort(String.valueOf(greenMail.getSmtp().getPort()));
        cfg.setUsername("legacyUser");
        cfg.setPa55word("legacyPw");
        cfg.setSmtpAuthEnable(true);
        cfg.setConnectionTimeout(1000);
        cfg.setReadTimeout(1000);
        cfg.setWriteTimeout(1000);

        assertThat(cfg.getServers()).isEmpty();

        MailServiceImpl service = new MailServiceImpl(cfg);

        assertThatCode(service::setInitData).doesNotThrowAnyException();
        assertThatCode(() -> service.simpleMailSend(plainTextMail("SC-14"))).doesNotThrowAnyException();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-14");
    }
}
