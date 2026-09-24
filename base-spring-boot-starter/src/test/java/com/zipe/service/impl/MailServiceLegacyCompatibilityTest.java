package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.autoconfiguration.BaseAutoConfiguration;
import com.zipe.config.MailPropertyConfig;
import com.zipe.service.MailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
    void flatLegacyProperties_withoutServersList_stillInitializesAndSendsSuccessfully() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BaseAutoConfiguration.class))
                .withPropertyValues(
                        "mail.host=127.0.0.1",
                        "mail.port=" + greenMail.getSmtp().getPort(),
                        "mail.username=legacyUser",
                        "mail.pa55word=legacyPw",
                        "mail.smtp-auth-enable=true",
                        "mail.connection-timeout=1000",
                        "mail.read-timeout=1000",
                        "mail.write-timeout=1000")
                .run(context -> {
                    assertThat(context).hasSingleBean(MailPropertyConfig.class);
                    assertThat(context).hasSingleBean(MailService.class);

                    MailPropertyConfig cfg = context.getBean(MailPropertyConfig.class);
                    assertThat(cfg.getServers()).isEmpty();
                    assertThat(cfg.getHost()).isEqualTo("127.0.0.1");
                    assertThat(cfg.getPort()).isEqualTo(String.valueOf(greenMail.getSmtp().getPort()));
                    assertThat(cfg.getUsername()).isEqualTo("legacyUser");

                    MailService service = context.getBean(MailService.class);
                    assertThatCode(service::setInitData).doesNotThrowAnyException();
                    assertThatCode(() -> service.simpleMailSend(plainTextMail("SC-14")))
                            .doesNotThrowAnyException();

                    MimeMessage[] messages = greenMail.getReceivedMessages();
                    assertThat(messages).hasSize(1);
                    assertThatCode(() -> assertThat(messages[0].getSubject()).isEqualTo("SC-14"))
                            .doesNotThrowAnyException();
                });
    }

    /**
     * SC-036：逐一核對全部 10 個既有扁平 {@code mail.*} 設定鍵仍可正確繫結且語意不變
     * （AC-011-02），而非僅驗證其中一部分欄位。每個鍵皆設為非預設值，並核對
     * {@link MailPropertyConfig#resolveServers()} 合成的唯一一組候選確實逐一採用這些值。
     */
    @Test
    void allTenLegacyFlatKeys_bindCorrectly_andAreUsedByResolveServers() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BaseAutoConfiguration.class))
                .withPropertyValues(
                        "mail.host=legacy-host.example.test",
                        "mail.port=2525",
                        "mail.username=legacy-user",
                        "mail.pa55word=legacy-pass",
                        "mail.sender=legacy-sender@example.test",
                        "mail.smtp-auth-enable=false",
                        "mail.smtp-start-tls-enable=true",
                        "mail.transport-protocol=smtps",
                        "mail.encrypt-enable=true",
                        "mail.debug-enable=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(MailPropertyConfig.class);
                    MailPropertyConfig cfg = context.getBean(MailPropertyConfig.class);

                    assertThat(cfg.getHost()).isEqualTo("legacy-host.example.test");
                    assertThat(cfg.getPort()).isEqualTo("2525");
                    assertThat(cfg.getUsername()).isEqualTo("legacy-user");
                    assertThat(cfg.getPa55word()).isEqualTo("legacy-pass");
                    assertThat(cfg.getSender()).isEqualTo("legacy-sender@example.test");
                    assertThat(cfg.getSmtpAuthEnable()).isFalse();
                    assertThat(cfg.getSmtpStartTlsEnable()).isTrue();
                    assertThat(cfg.getTransportProtocol()).isEqualTo("smtps");
                    assertThat(cfg.getEncryptEnable()).isTrue();
                    assertThat(cfg.getDebugEnable()).isTrue();

                    var candidates = cfg.resolveServers();
                    assertThat(candidates).hasSize(1);
                    var onlyCandidate = candidates.get(0);
                    assertThat(onlyCandidate.getHost()).isEqualTo("legacy-host.example.test");
                    assertThat(onlyCandidate.getPort()).isEqualTo("2525");
                    assertThat(onlyCandidate.getUsername()).isEqualTo("legacy-user");
                    assertThat(onlyCandidate.getPa55word()).isEqualTo("legacy-pass");
                    assertThat(onlyCandidate.getSmtpAuthEnable()).isFalse();
                    assertThat(onlyCandidate.getSmtpStartTlsEnable()).isTrue();
                    assertThat(onlyCandidate.getTransportProtocol()).isEqualTo("smtps");
                    assertThat(onlyCandidate.getEncryptEnable()).isTrue();
                });
    }
}
