package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static com.zipe.service.impl.MailFailoverTestSupport.unreachableServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.exception.MailFailoverException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 逾時設定與嘗試組數上限情境測試，對應情境測試計畫 SC-18、SC-19、SC-26。
 *
 * <p>對應 REQ-MAIL-FAILOVER-010：須維持可設定的連線／讀取／寫入逾時，並限制整體切換行為的
 * 最大嘗試組數或整體切換時間上限，避免無上限累加阻塞呼叫端。</p>
 */
class MailServiceTimeoutAndLimitsTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("backupUser", "backupPw"));

    private ServerSocket blackHole;

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void closeBlackHole() throws IOException {
        if (blackHole != null && !blackHole.isClosed()) {
            blackHole.close();
        }
    }

    /** SC-19（預設值）：未設定逾時屬性時，MailPropertyConfig 的預設值與文件記載的 5000/3000/5000 一致。 */
    @Test
    void defaultTimeouts_matchDocumentedValues() {
        MailPropertyConfig config = new MailPropertyConfig();
        assertThat(config.getConnectionTimeout()).isEqualTo(5000);
        assertThat(config.getReadTimeout()).isEqualTo(3000);
        assertThat(config.getWriteTimeout()).isEqualTo(5000);
    }

    /** SC-19：預設值與自訂值皆須實際寫入候選 JavaMailSender，而非只停留在設定物件。 */
    @Test
    void timeoutProperties_areAppliedToInitializedJavaMailSenders() throws Exception {
        MailPropertyConfig defaults = new MailPropertyConfig();
        defaults.getServers().add(
                greenMailServer("default", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        assertTimeouts(initializedSender(defaults), 5000, 3000, 5000);

        MailPropertyConfig custom = new MailPropertyConfig();
        custom.getServers().add(
                greenMailServer("custom", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        custom.setConnectionTimeout(1000);
        custom.setReadTimeout(800);
        custom.setWriteTimeout(1000);
        assertTimeouts(initializedSender(custom), 1000, 800, 1000);
    }

    /** SC-19（可設定）：讀取逾時可由設定屬性調整，覆寫後以較小值生效（以實際失敗耗時佐證，而非仍套用預設 3000ms）。 */
    @Test
    void readTimeout_isConfigurable_andTakesEffect() throws Exception {
        int port = startBlackHoleServer();
        MailServerProperty server = new MailServerProperty();
        server.setName("hanging");
        server.setHost("127.0.0.1");
        server.setPort(String.valueOf(port));
        server.setSmtpAuthEnable(false);

        MailPropertyConfig cfg = config(server);
        cfg.setReadTimeout(300);
        cfg.setConnectionTimeout(1000);
        cfg.setWriteTimeout(1000);
        MailServiceImpl service = new MailServiceImpl(cfg);

        long start = System.currentTimeMillis();
        assertThatThrownBy(service::setInitData).isInstanceOf(jakarta.mail.MessagingException.class);
        long elapsed = System.currentTimeMillis() - start;

        // 預設 readTimeout 為 3000ms；此處覆寫為 300ms，理應遠早於 3000ms 失敗，證明設定值確實生效
        assertThat(elapsed).isLessThan(2000);
    }

    /** SC-18：全部 SMTP 皆不可達（連線可建立但無回應）時，整體切換時間上限可限制單次呼叫的總耗時。 */
    @Test
    void overallTimeout_boundsTotalElapsedTime_evenWithManyHangingServers() throws Exception {
        int port = startBlackHoleServer();

        MailPropertyConfig cfg = new MailPropertyConfig();
        for (int i = 0; i < 8; i++) {
            MailServerProperty server = new MailServerProperty();
            server.setName("hanging-" + i);
            server.setHost("127.0.0.1");
            server.setPort(String.valueOf(port));
            server.setSmtpAuthEnable(false);
            cfg.getServers().add(server);
        }
        cfg.setReadTimeout(300);
        cfg.setConnectionTimeout(1000);
        cfg.setWriteTimeout(1000);
        cfg.getFailover().setOverallTimeout(700L);

        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (jakarta.mail.MessagingException ignored) {
            // 全部候選在初始化階段皆會逾時失敗，仍保留候選清單供後續發送嘗試
        }

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-18"))).isInstanceOf(MailFailoverException.class);
        long elapsed = System.currentTimeMillis() - start;

        // 若無整體上限，8 組個別耗時 300ms 累加將達 2400ms；有整體上限（700ms）時應明顯低於此值
        assertThat(elapsed).isLessThanOrEqualTo(1200);
    }

    /** SC-26：可設定之最大嘗試組數小於已設定組數時，超過上限之組別不會被嘗試。 */
    @Test
    void maxAttempts_belowConfiguredServerCount_stopsBeforeRemainingServers() throws Exception {
        MailPropertyConfig cfg = config(
                unreachableServer("primary", 1),
                unreachableServer("secondary", 2),
                greenMailServer("tertiary", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        cfg.getFailover().setMaxAttempts(2);

        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-26")))
                .isInstanceOf(MailFailoverException.class)
                .hasMessageContaining("primary")
                .hasMessageContaining("secondary")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("tertiary"));

        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    /** 建立一個「可連線但永不回應」的本機伺服器，模擬會拖到讀取逾時才失敗的 SMTP 伺服器。 */
    private int startBlackHoleServer() throws IOException {
        blackHole = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        int port = blackHole.getLocalPort();
        Thread acceptor = new Thread(() -> {
            while (!blackHole.isClosed()) {
                try {
                    Socket socket = blackHole.accept();
                    // 刻意不寫回任何位元組，讓客戶端等待初始問候語直到讀取逾時
                    socket.getClass();
                } catch (IOException ignored) {
                    // ServerSocket 關閉時 accept() 拋出例外屬正常結束
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        return port;
    }

    private static JavaMailSenderImpl initializedSender(MailPropertyConfig config) throws Exception {
        MailServiceImpl service = new MailServiceImpl(config);
        service.setInitData();
        List<?> candidates = (List<?>) ReflectionTestUtils.getField(service, "candidates");
        assertThat(candidates).isNotNull().hasSize(1);
        return (JavaMailSenderImpl) ReflectionTestUtils.getField(candidates.get(0), "sender");
    }

    private static void assertTimeouts(
            JavaMailSenderImpl sender, int connectionTimeout, int readTimeout, int writeTimeout) {
        assertThat(String.valueOf(sender.getJavaMailProperties().get("mail.smtp.connectiontimeout")))
                .isEqualTo(String.valueOf(connectionTimeout));
        assertThat(String.valueOf(sender.getJavaMailProperties().get("mail.smtp.timeout")))
                .isEqualTo(String.valueOf(readTimeout));
        assertThat(String.valueOf(sender.getJavaMailProperties().get("mail.smtp.writetimeout")))
                .isEqualTo(String.valueOf(writeTimeout));
    }
}
