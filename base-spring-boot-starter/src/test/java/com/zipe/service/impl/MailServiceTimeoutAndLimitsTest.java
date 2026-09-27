package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static com.zipe.service.impl.MailFailoverTestSupport.unreachableServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.exception.MailFailoverException;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.SocketFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
    private final AtomicInteger blackHoleConnections = new AtomicInteger();
    private final List<Socket> heldSockets = new CopyOnWriteArrayList<>();
    private MultiStageDelaySmtpServer delayedSmtp;
    private MultiStageDelaySmtpServer backupSmtp;

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void closeBlackHole() throws IOException {
        if (blackHole != null && !blackHole.isClosed()) {
            blackHole.close();
        }
        for (Socket socket : heldSockets) {
            socket.close();
        }
        if (delayedSmtp != null) {
            delayedSmtp.close();
        }
        if (backupSmtp != null) {
            backupSmtp.close();
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

    /**
     * SC-020（AC-008-01／02）：預設值與自訂值皆須實際寫入「每一組」候選 JavaMailSender，而非只停留在
     * 設定物件或只套用第一組；三組候選逐組精確核對連線、讀取、寫入三鍵。覆寫值三者互異，
     * 可攔截 read／write 對調或後續候選遺漏任一鍵的實作。
     */
    @Test
    void timeoutProperties_areAppliedToEveryInitializedJavaMailSender() throws Exception {
        MailPropertyConfig defaults = threeGreenMailServers("default");
        List<JavaMailSenderImpl> defaultSenders = initializedSenders(defaults, 3);
        for (JavaMailSenderImpl sender : defaultSenders) {
            assertTimeouts(sender, 5000, 3000, 5000);
        }

        MailPropertyConfig custom = threeGreenMailServers("custom");
        custom.setConnectionTimeout(1100);
        custom.setReadTimeout(700);
        custom.setWriteTimeout(1300);
        List<JavaMailSenderImpl> customSenders = initializedSenders(custom, 3);
        for (JavaMailSenderImpl sender : customSenders) {
            assertTimeouts(sender, 1100, 700, 1300);
        }
    }

    private static MailPropertyConfig threeGreenMailServers(String prefix) {
        MailPropertyConfig cfg = new MailPropertyConfig();
        for (int i = 1; i <= 3; i++) {
            cfg.getServers().add(
                    greenMailServer(prefix + "-" + i, greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        }
        return cfg;
    }

    /** 非數字的底層 timeout 屬性不得使 failover 本身失效，應安全縮限為剩餘整體時間。 */
    @Test
    void malformedSenderTimeout_fallsBackToOverallDeadline() throws Exception {
        MailPropertyConfig cfg = config(
                greenMailServer("malformed-timeout", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        cfg.getFailover().setOverallTimeout(1000L);
        MailServiceImpl service = initializedService(cfg);
        List<?> candidates = (List<?>) ReflectionTestUtils.getField(service, "candidates");
        JavaMailSenderImpl sender = (JavaMailSenderImpl) ReflectionTestUtils.getField(candidates.get(0), "sender");
        sender.getJavaMailProperties().put("mail.smtp.timeout", "not-a-number");

        service.simpleMailSend(plainTextMail("malformed-timeout"));

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    /**
     * AC-011-03：未設定 mail.failover.* 時，max-attempts 預設等同不額外限制（即嘗試全部已設定組數），
     * overall-timeout 預設為 30000 毫秒，維持升級前單組行為所需的相容性基準值。
     */
    @Test
    void defaultFailoverProperties_matchDocumentedValues() {
        MailPropertyConfig config = new MailPropertyConfig();
        assertThat(config.getFailover().getMaxAttempts()).isEqualTo(Integer.MAX_VALUE);
        assertThat(config.getFailover().getOverallTimeout()).isEqualTo(30000L);
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

    /**
     * SC-18：全部 SMTP 皆不可達（連線可建立但無回應）時，整體切換時間上限可限制單次呼叫的總耗時，
     * 且錯誤訊息須明示為整體逾時。重複量測 3 次，排除單次執行的偶發排程抖動。
     */
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

        for (int attempt = 0; attempt < 3; attempt++) {
            String subject = "SC-18-" + attempt;
            long start = System.nanoTime();
            assertThatThrownBy(() -> service.simpleMailSend(plainTextMail(subject)))
                    .isInstanceOf(MailFailoverException.class)
                    .hasMessageContaining("整體逾時");
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            // 若無整體上限，8 組個別耗時 300ms 累加將達 2400ms；有整體上限（700ms）時應明顯低於此值。
            // 下限：300ms 的讀取逾時短於剩餘預算，不得被誤判為整體逾時而提前停止切換，
            // 故必須實際耗盡 700ms 的整體預算才結束（絕對截止時間不會早於起點 + 700ms）。
            assertThat(elapsed).isBetween(690L, 1200L);
        }
    }

    /**
     * SC-030：單一 SMTP 階段的逾時（3000ms）長於 overall-timeout（700ms）時，流程須在整體上限附近截止
     * （不早於上限、不超過容忍上限），失敗原因標示為整體逾時，且後續可用組於發送階段連線數為 0。
     * 重複量測三次，排除單次排程抖動。
     */
    @Test
    void overallTimeout_singleStageLongerThanBudget_stopsNearDeadline_andNeverTouchesLaterServer() throws Exception {
        int port = startBlackHoleServer();
        backupSmtp = new MultiStageDelaySmtpServer(0);
        MailPropertyConfig cfg = config(
                hangingServer("hanging-first", port), hangingServer("usable-second", backupSmtp.port()));
        cfg.setReadTimeout(3000);
        cfg.setConnectionTimeout(3000);
        cfg.setWriteTimeout(3000);
        cfg.getFailover().setOverallTimeout(700L);
        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (jakarta.mail.MessagingException ignored) {
            // 第一組初始化時亦會逾時；保留候選清單供發送嘗試
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            int connectionsBeforeSend = backupSmtp.connectionCount();
            String subject = "SC-030-" + attempt;
            long start = System.nanoTime();
            assertThatThrownBy(() -> service.simpleMailSend(plainTextMail(subject)))
                    .isInstanceOf(MailFailoverException.class)
                    .hasMessageContaining("hanging-first")
                    .hasMessageContaining("整體逾時（overall-timeout）")
                    .hasMessageContaining("已嘗試 1 組")
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain("usable-second"));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            assertThat(elapsed).as("第 %d 次：不得早於整體上限，亦不得超過容忍上限", attempt).isBetween(690L, 1200L);
            assertThat(backupSmtp.connectionCount())
                    .as("整體預算耗盡後不得再連線後續可用組")
                    .isEqualTo(connectionsBeforeSend);
            assertThat(backupSmtp.acceptedMessages()).isZero();
        }
    }

    /**
     * SC-031：某組實際的讀取逾時（300ms）短於剩餘整體預算（1500ms）時，即使連線／寫入逾時（3000ms）
     * 因剩餘預算較短而被縮限，該次讀取逾時仍屬該伺服器本身的失敗：必須繼續切換至下一組並送達，
     * 耗時不得早於讀取逾時，且第一組的失敗原因不得被標示為整體逾時。
     * <p>
     * 計畫原始數值為 read=300／connection、write=1000／overall=700；實測第一組（TLS 探測＋明文讀取逾時）
     * 約耗 540ms，剩餘約 160ms 不足以讓備援組完成首次投遞，屬時間門檻過緊。此處等比放寬為
     * read=300／connection、write=3000／overall=1500，仍維持「讀取逾時 &lt; 剩餘預算 &lt; 其他單項逾時」
     * 的交叉條件：以「任一單項逾時被縮限即判定整體逾時」的弱實作，仍會在第一組讀取逾時時提前停止而失敗。
     * </p>
     */
    @Test
    void readTimeoutShorterThanRemainingBudget_isNotOverallTimeout_andFailsOverToNextServer() throws Exception {
        int port = startBlackHoleServer();
        MailPropertyConfig cfg = config(
                hangingServer("read-timeout-first", port),
                greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        cfg.setReadTimeout(300);
        cfg.setConnectionTimeout(3000);
        cfg.setWriteTimeout(3000);
        cfg.getFailover().setOverallTimeout(1500L);
        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logs =
                MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);
        try {
            long start = System.nanoTime();
            assertThatCode(() -> service.simpleMailSend(plainTextMail("SC-031-read-timeout")))
                    .doesNotThrowAnyException();
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            assertThat(elapsed).as("第一組須實際等到讀取逾時才切換，不得提前停止").isGreaterThanOrEqualTo(300L);
            MimeMessage[] messages = greenMail.getReceivedMessages();
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).isEqualTo("SC-031-read-timeout");

            List<String> messagesLogged = logs.list.stream()
                    .map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                    .toList();
            assertThat(messagesLogged)
                    .anySatisfy(line -> assertThat(line)
                            .contains("郵件發送失敗（simpleMailSend）")
                            .contains("read-timeout-first")
                            .contains("將嘗試下一組"))
                    .anySatisfy(line -> assertThat(line)
                            .contains("郵件發送成功（simpleMailSend）")
                            .contains("backup"))
                    .noneSatisfy(line -> assertThat(line).contains("整體時間上限"));
        } finally {
            ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(MailServiceImpl.class))
                    .detachAppender(logs);
        }
    }

    /**
     * SC-024（AC-008-07）：單次 SMTP 對話包含多個各自未逾時的延遲階段（MAIL FROM、RCPT TO 各 400ms，
     * 均低於 2000ms 單項逾時），累積超過 overall-timeout（700ms）時，呼叫須在截止上下限內結束；
     * 截止時在途 socket 必須被關閉，使 SMTP 對話停在截止前的階段（不得進入 DATA、零投遞），
     * 且後續可用組零連線。重複三次，排除單次排程抖動。
     */
    @Test
    void overallTimeout_boundsWholeOperation_acrossMultipleSmtpStages() throws Exception {
        delayedSmtp = new MultiStageDelaySmtpServer(400);
        backupSmtp = new MultiStageDelaySmtpServer(0);
        MailServerProperty server = new MailServerProperty();
        server.setName("multi-stage-delay");
        server.setHost("127.0.0.1");
        server.setPort(String.valueOf(delayedSmtp.port()));
        server.setSmtpAuthEnable(false);
        MailServerProperty backup = new MailServerProperty();
        backup.setName("usable-after-deadline");
        backup.setHost("127.0.0.1");
        backup.setPort(String.valueOf(backupSmtp.port()));
        backup.setSmtpAuthEnable(false);

        MailPropertyConfig cfg = config(server, backup);
        cfg.setReadTimeout(2000);
        cfg.setConnectionTimeout(2000);
        cfg.setWriteTimeout(2000);
        cfg.getFailover().setOverallTimeout(700L);

        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        for (int attempt = 0; attempt < 3; attempt++) {
            int commandsBeforeSend = delayedSmtp.commands().size();
            int backupConnectionsBeforeSend = backupSmtp.connectionCount();
            String subject = "SC-024-multi-stage-" + attempt;

            long startedNanos = System.nanoTime();
            assertThatThrownBy(() -> service.simpleMailSend(plainTextMail(subject)))
                    .isInstanceOf(MailFailoverException.class)
                    .hasMessageContaining("multi-stage-delay")
                    .hasMessageContaining("整體逾時（overall-timeout）")
                    .hasMessageContaining("已嘗試 1 組")
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain("usable-after-deadline"));
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);

            // 每個回應只延遲 400ms，均低於個別 read timeout；若只限制各 socket 操作，累積會超過 1 秒。
            // 下限：絕對截止時間不會早於起點 + 700ms，提前停止的弱實作會低於此值。
            assertThat(elapsedMs).as("第 %d 次：須在整體截止上下限內結束", attempt).isBetween(690L, 1200L);

            // 截止時 DeadlineSocketFactory 須關閉在途 socket；伺服器端連線須隨之結束，
            // 背景執行緒不得在呼叫端收到失敗後繼續完成 SMTP 對話。
            assertThat(delayedSmtp.awaitNoClients(800))
                    .as("第 %d 次：截止後在途連線須被關閉，實際指令：%s", attempt, delayedSmtp.commands())
                    .isTrue();
            List<String> sendCommands =
                    delayedSmtp.commands().subList(commandsBeforeSend, delayedSmtp.commands().size());
            assertThat(sendCommands)
                    .as("第 %d 次：須實際進入多個受控延遲階段", attempt)
                    .contains("MAIL", "RCPT")
                    .as("第 %d 次：截止後不得繼續進入 DATA 階段", attempt)
                    .doesNotContain("DATA");
            assertThat(delayedSmtp.acceptedMessages()).as("截止後不得在背景完成投遞").isZero();
            assertThat(backupSmtp.connectionCount())
                    .as("第 %d 次：整體預算耗盡後不得再連線後續可用組", attempt)
                    .isEqualTo(backupConnectionsBeforeSend);
            assertThat(backupSmtp.acceptedMessages()).isZero();
        }
    }

    /** 呼叫端中斷等待時須保留 interrupt 狀態並停止嘗試下一組 SMTP。 */
    @Test
    void interruptedSend_preservesInterruptAndStopsFailover() throws Exception {
        delayedSmtp = new MultiStageDelaySmtpServer(2000);
        MailServerProperty server = new MailServerProperty();
        server.setName("interrupted");
        server.setHost("127.0.0.1");
        server.setPort(String.valueOf(delayedSmtp.port()));
        server.setSmtpAuthEnable(false);
        MailPropertyConfig cfg = config(server);
        cfg.setReadTimeout(5000);
        cfg.getFailover().setOverallTimeout(5000L);
        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread worker = new Thread(() -> {
            try {
                service.simpleMailSend(plainTextMail("interrupted-send"));
            } catch (Throwable e) {
                failure.set(e);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        worker.start();
        assertThat(delayedSmtp.awaitConnections(2, 1000)).isTrue();
        worker.interrupt();
        worker.join(1500);

        assertThat(worker.isAlive()).isFalse();
        assertThat(failure.get()).isInstanceOf(MailFailoverException.class);
        assertThat(interrupted.get()).isTrue();
    }

    /** overall-timeout=0 表示不限制整段寄送時間，仍須沿用原本同步寄送路徑。 */
    @Test
    void overallTimeout_zero_keepsUnboundedSendPath() throws Exception {
        MailPropertyConfig cfg = config(
                greenMailServer("unbounded", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        cfg.getFailover().setOverallTimeout(0L);
        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        service.simpleMailSend(plainTextMail("unbounded-send"));

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    /** SC-034（0 邊界）：停用整體上限不得連帶停用各候選的 read-timeout，三組黑洞皆須被嘗試後結束。 */
    @Test
    @Timeout(10)
    void overallTimeout_zero_stillUsesPerServerTimeout_andAttemptsEveryCandidate() throws Exception {
        assertUnboundedOverallTimeoutStillUsesPerServerTimeout(0L, "zero");
    }

    /**
     * SC-031（負值邊界）：overall-timeout 為負值時比照 0，視為不限制整體耗時，而非被誤判為
     * 「立即逾時」或被傳入排程器／{@code TimeUnit} 換算成負數等待時間導致例外或行為異常；
     * 各組仍受各自的連線／讀取／寫入逾時限制（本測試以正常送達證明兩者皆成立）。
     */
    @Test
    void overallTimeout_negative_keepsUnboundedSendPath_andDoesNotThrowOrMisbehave() throws Exception {
        MailPropertyConfig cfg = config(
                greenMailServer("unbounded-negative", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        cfg.getFailover().setOverallTimeout(-1L);
        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        assertThatCode(() -> service.simpleMailSend(plainTextMail("unbounded-negative-send")))
                .doesNotThrowAnyException();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("unbounded-negative-send");
    }

    /**
     * SC-031（負值邊界，失敗路徑）：overall-timeout 為負值且全部伺服器皆不可達時，仍須如「不限制整體
     * 耗時」語意逐組嘗試至候選清單結束，而非因負值被誤判為 0 次嘗試或立即以整體逾時原因結束；
     * 失敗摘要須包含全部組別、且不得誤標為 overall-timeout 原因。
     */
    @Test
    void overallTimeout_negative_stillAttemptsAllCandidates_whenAllUnavailable() throws Exception {
        MailPropertyConfig cfg = config(unreachableServer("primary", 1), unreachableServer("secondary", 2));
        cfg.getFailover().setOverallTimeout(-100L);
        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (jakarta.mail.MessagingException ignored) {
            // 兩組皆不可用，仍保留候選清單供後續發送嘗試
        }

        assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-031-negative-all-down")))
                .isInstanceOf(MailFailoverException.class)
                .hasMessageContaining("primary")
                .hasMessageContaining("secondary")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("整體逾時"));
    }

    /** SC-034（負值邊界）：負值與 0 相同，僅停用 overall-timeout，各候選的 read-timeout 仍須生效。 */
    @Test
    @Timeout(10)
    void overallTimeout_negative_stillUsesPerServerTimeout_andAttemptsEveryCandidate() throws Exception {
        assertUnboundedOverallTimeoutStillUsesPerServerTimeout(-1L, "negative");
    }

    /** SocketFactory 的所有連線建立入口都必須受到同一絕對截止時間保護。 */
    @Test
    void deadlineSocketFactory_guardsEveryConnectedSocketCreationVariant() throws Exception {
        Class<?> factoryType = Class.forName(MailServiceImpl.class.getName() + "$DeadlineSocketFactory");
        var constructor = factoryType.getDeclaredConstructor(
                long.class, SocketFactory.class, boolean.class, int.class, String.class, int.class, boolean.class);
        constructor.setAccessible(true);

        List<Socket> clients = new ArrayList<>();
        List<Socket> peers = new ArrayList<>();
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (ServerSocket listener = new ServerSocket(0, 50, loopback)) {
            SocketFactory factory = (SocketFactory) constructor.newInstance(
                    System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500),
                    SocketFactory.getDefault(),
                    true,
                    2000,
                    "127.0.0.1",
                    listener.getLocalPort(),
                    false);

            clients.add(factory.createSocket("127.0.0.1", listener.getLocalPort()));
            peers.add(listener.accept());
            clients.add(factory.createSocket("127.0.0.1", listener.getLocalPort(), loopback, 0));
            peers.add(listener.accept());
            clients.add(factory.createSocket(loopback, listener.getLocalPort()));
            peers.add(listener.accept());
            clients.add(factory.createSocket(loopback, listener.getLocalPort(), loopback, 0));
            peers.add(listener.accept());

            long waitUntil = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (clients.stream().anyMatch(socket -> !socket.isClosed()) && System.nanoTime() < waitUntil) {
                Thread.sleep(10);
            }
            assertThat(clients).allMatch(Socket::isClosed);

            var closeQuietly = factoryType.getDeclaredMethod("closeQuietly", Socket.class);
            closeQuietly.setAccessible(true);
            closeQuietly.invoke(null, new Socket() {
                @Override
                public synchronized void close() throws IOException {
                    throw new IOException("expected close failure");
                }
            });
        } finally {
            for (Socket socket : clients) {
                socket.close();
            }
            for (Socket socket : peers) {
                socket.close();
            }
        }
    }

    /**
     * CONFIRMED HIGH 回歸測試：截止機制不得以一般 TCP socket 取代原設定（例如隱式 TLS 的
     * {@link javax.net.ssl.SSLSocketFactory}）委派工廠建立的 socket；實際連線的建立者
     * 必須是委派工廠本身，僅另外掛上到期關閉排程。
     */
    @Test
    void deadlineSocketFactory_delegatesActualSocketCreation_preservingTlsSemantics() throws Exception {
        Class<?> factoryType = Class.forName(MailServiceImpl.class.getName() + "$DeadlineSocketFactory");
        var constructor = factoryType.getDeclaredConstructor(
                long.class, SocketFactory.class, boolean.class, int.class, String.class, int.class, boolean.class);
        constructor.setAccessible(true);

        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (ServerSocket listener = new ServerSocket(0, 50, loopback)) {
            AtomicInteger delegateInvocations = new AtomicInteger();
            AtomicReference<Socket> delegateProducedSocket = new AtomicReference<>();
            // 實際連線由本工廠自行以 connect(SocketAddress, timeout) 完成，委派工廠只需負責
            // 建立「未連線」的 socket 實例（等同 SSLSocketFactory#createSocket() 無參數版本）。
            SocketFactory recordingDelegate = new SocketFactory() {
                @Override
                public Socket createSocket() {
                    delegateInvocations.incrementAndGet();
                    Socket socket = new Socket();
                    delegateProducedSocket.set(socket);
                    return socket;
                }

                @Override
                public Socket createSocket(String host, int port) {
                    throw new UnsupportedOperationException("不應呼叫此變體，連線應由委派工廠自行 connect()");
                }

                @Override
                public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) {
                    throw new UnsupportedOperationException("不應呼叫此變體，連線應由委派工廠自行 connect()");
                }

                @Override
                public Socket createSocket(InetAddress host, int port) {
                    throw new UnsupportedOperationException("不應呼叫此變體，連線應由委派工廠自行 connect()");
                }

                @Override
                public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) {
                    throw new UnsupportedOperationException("不應呼叫此變體，連線應由委派工廠自行 connect()");
                }
            };

            SocketFactory factory = (SocketFactory) constructor.newInstance(
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(30),
                    recordingDelegate,
                    true,
                    2000,
                    "127.0.0.1",
                    listener.getLocalPort(),
                    false);

            Socket client = factory.createSocket("127.0.0.1", listener.getLocalPort());
            try (Socket peer = listener.accept()) {
                assertThat(delegateInvocations.get()).isEqualTo(1);
                assertThat(client).isSameAs(delegateProducedSocket.get());
                assertThat(client.isConnected()).isTrue();
            } finally {
                client.close();
            }
        }
    }

    /** 委派工廠建立失敗且允許退回時，須改用一般 TCP socket 完成連線（對應 STARTTLS 場景的既有 fallback 行為）。 */
    @Test
    void deadlineSocketFactory_fallsBackToPlainSocket_whenDelegateFailsAndFallbackEnabled() throws Exception {
        Class<?> factoryType = Class.forName(MailServiceImpl.class.getName() + "$DeadlineSocketFactory");
        var constructor = factoryType.getDeclaredConstructor(
                long.class, SocketFactory.class, boolean.class, int.class, String.class, int.class, boolean.class);
        constructor.setAccessible(true);

        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (ServerSocket listener = new ServerSocket(0, 50, loopback)) {
            SocketFactory factory = (SocketFactory) constructor.newInstance(
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(30),
                    alwaysFailingSocketFactory(),
                    true,
                    2000,
                    "127.0.0.1",
                    listener.getLocalPort(),
                    false);

            try (Socket client = factory.createSocket("127.0.0.1", listener.getLocalPort());
                    Socket peer = listener.accept()) {
                assertThat(client.isConnected()).isTrue();
            }
        }
    }

    /** 委派工廠建立失敗且未允許退回時，須直接拋出例外，不得靜默改用一般 TCP socket。 */
    @Test
    void deadlineSocketFactory_propagatesDelegateFailure_whenFallbackDisabled() throws Exception {
        Class<?> factoryType = Class.forName(MailServiceImpl.class.getName() + "$DeadlineSocketFactory");
        var constructor = factoryType.getDeclaredConstructor(
                long.class, SocketFactory.class, boolean.class, int.class, String.class, int.class, boolean.class);
        constructor.setAccessible(true);

        SocketFactory factory = (SocketFactory) constructor.newInstance(
                System.nanoTime() + TimeUnit.SECONDS.toNanos(30),
                alwaysFailingSocketFactory(),
                false,
                2000,
                "127.0.0.1",
                1,
                false);

        assertThatThrownBy(() -> factory.createSocket("127.0.0.1", 1))
                .isInstanceOf(IOException.class)
                .hasMessage("delegate unavailable");
    }

    /** 建立一個 {@code createSocket()}（無參數版本）恆丟出 IOException 的委派工廠，供 fallback 情境測試使用。 */
    private static SocketFactory alwaysFailingSocketFactory() {
        return new SocketFactory() {
            @Override
            public Socket createSocket() throws IOException {
                throw new IOException("delegate unavailable");
            }

            @Override
            public Socket createSocket(String host, int port) throws IOException {
                throw new UnsupportedOperationException("不應呼叫此變體，連線應由委派工廠自行 connect()");
            }

            @Override
            public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) {
                throw new UnsupportedOperationException("不應呼叫此變體，連線應由委派工廠自行 connect()");
            }

            @Override
            public Socket createSocket(InetAddress host, int port) {
                throw new UnsupportedOperationException("不應呼叫此變體，連線應由委派工廠自行 connect()");
            }

            @Override
            public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) {
                throw new UnsupportedOperationException("不應呼叫此變體，連線應由委派工廠自行 connect()");
            }
        };
    }

    /** SC-26：可設定之最大嘗試組數小於已設定組數時，超過上限之組別不會被嘗試。 */
    @Test
    void maxAttempts_belowConfiguredServerCount_stopsBeforeRemainingServers() throws Exception {
        try (MailFailoverTestSupport.CountingTcpServer tertiary =
                new MailFailoverTestSupport.CountingTcpServer()) {
            MailPropertyConfig cfg = config(
                    unreachableServer("primary", 1),
                    unreachableServer("secondary", 2),
                    MailFailoverTestSupport.server(
                            "tertiary", "127.0.0.1", tertiary.port(), "tertiary-user", "tertiary-password"));
            cfg.getFailover().setMaxAttempts(2);

            MailServiceImpl service = new MailServiceImpl(cfg);
            assertThatThrownBy(service::setInitData).isInstanceOf(jakarta.mail.MessagingException.class);
            tertiary.resetCount();

            assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-26")))
                    .isInstanceOf(MailFailoverException.class)
                    .hasMessageContaining("primary")
                    .hasMessageContaining("secondary")
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain("tertiary"));

            tertiary.waitBriefly(200);
            assertThat(tertiary.connectionCount())
                    .as("max-attempts=2 時第三組在發送階段不得建立任何連線")
                    .isZero();
            assertThat(greenMail.getReceivedMessages()).isEmpty();
        }
    }

    /**
     * 建立一個「可連線但永不回應」的本機伺服器，模擬會拖到讀取逾時才失敗的 SMTP 伺服器。
     * <p>
     * 明文 smtp 組別的截止工廠會先以 TLS handshake 探測對方（客戶端主動送出 ClientHello），
     * 失敗後才退回明文連線。本伺服器收到任何客戶端位元組（即 TLS 探測）時立即關閉該連線，
     * 讓探測快速失敗；明文連線則因客戶端等待問候語、不會送出資料而一直卡住，直到客戶端的
     * 讀取逾時到期。每個已接受的連線皆保留強參考，避免被 GC 回收而提前關閉、使逾時時間失真。
     * </p>
     */
    private int startBlackHoleServer() throws IOException {
        blackHole = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        int port = blackHole.getLocalPort();
        Thread acceptor = new Thread(() -> {
            while (!blackHole.isClosed()) {
                try {
                    Socket socket = blackHole.accept();
                    blackHoleConnections.incrementAndGet();
                    heldSockets.add(socket);
                    Thread holder = new Thread(() -> {
                        try (socket) {
                            // 刻意不寫回任何位元組；收到客戶端資料（TLS 探測）或對方關閉時才結束
                            socket.getInputStream().read();
                        } catch (IOException ignored) {
                            // 客戶端逾時關閉或測試結束關閉連線屬正常結束
                        }
                    }, "black-hole-holder");
                    holder.setDaemon(true);
                    holder.start();
                } catch (IOException ignored) {
                    // ServerSocket 關閉時 accept() 拋出例外屬正常結束
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        return port;
    }

    private void assertUnboundedOverallTimeoutStillUsesPerServerTimeout(long overallTimeout, String label)
            throws Exception {
        int port = startBlackHoleServer();
        MailPropertyConfig cfg = config(
                hangingServer(label + "-first", port),
                hangingServer(label + "-second", port),
                hangingServer(label + "-third", port));
        cfg.setConnectionTimeout(1000);
        cfg.setReadTimeout(250);
        cfg.setWriteTimeout(1000);
        cfg.getFailover().setOverallTimeout(overallTimeout);
        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (jakarta.mail.MessagingException ignored) {
            // 三組初始化皆會受 read-timeout 結束；保留候選清單供本次發送逐組驗證。
        }
        blackHoleConnections.set(0);

        long start = System.nanoTime();
        assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-034-" + label)))
                .isInstanceOf(MailFailoverException.class)
                .hasMessageContaining(label + "-first")
                .hasMessageContaining(label + "-second")
                .hasMessageContaining(label + "-third")
                .hasMessageContaining("已嘗試 3 組")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain("overall-timeout"));
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(elapsed)
                .as("三組都須各自等到 250ms read-timeout，且不能因停用單項 timeout 而永久卡住")
                .isBetween(700L, 2500L);
        assertThat(blackHoleConnections.get())
                .as("三組候選都必須建立實體連線；每組至少包含一次 TLS 探測及一次明文 SMTP 連線")
                .isGreaterThanOrEqualTo(6);
    }

    private static MailServerProperty hangingServer(String name, int port) {
        MailServerProperty server = new MailServerProperty();
        server.setName(name);
        server.setHost("127.0.0.1");
        server.setPort(String.valueOf(port));
        server.setSmtpAuthEnable(false);
        return server;
    }

    private static List<JavaMailSenderImpl> initializedSenders(MailPropertyConfig config, int expectedCount)
            throws Exception {
        MailServiceImpl service = initializedService(config);
        List<?> candidates = (List<?>) ReflectionTestUtils.getField(service, "candidates");
        assertThat(candidates).isNotNull().hasSize(expectedCount);
        List<JavaMailSenderImpl> senders = new ArrayList<>();
        for (Object candidate : candidates) {
            senders.add((JavaMailSenderImpl) ReflectionTestUtils.getField(candidate, "sender"));
        }
        assertThat(senders).doesNotContainNull().doesNotHaveDuplicates();
        return senders;
    }

    private static MailServiceImpl initializedService(MailPropertyConfig config) throws Exception {
        MailServiceImpl service = new MailServiceImpl(config);
        service.setInitData();
        return service;
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

    /** 最小可用 SMTP 伺服器：初始化連線立即回應，寄送連線則在多個協定階段分別延遲。 */
    private static final class MultiStageDelaySmtpServer implements AutoCloseable {

        private final int delayMs;
        private final ServerSocket serverSocket;
        private final List<Socket> clients = new CopyOnWriteArrayList<>();
        private final List<String> commands = new CopyOnWriteArrayList<>();
        private final AtomicInteger connectionCount = new AtomicInteger();
        private final AtomicInteger acceptedMessages = new AtomicInteger();

        private MultiStageDelaySmtpServer(int delayMs) throws IOException {
            this.delayMs = delayMs;
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            Thread acceptor = new Thread(this::acceptConnections, "multi-stage-smtp-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        private int port() {
            return serverSocket.getLocalPort();
        }

        private void acceptConnections() {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    clients.add(socket);
                    connectionCount.incrementAndGet();
                    Thread client = new Thread(() -> handle(socket), "multi-stage-smtp-client");
                    client.setDaemon(true);
                    client.start();
                } catch (IOException ignored) {
                    // close() 會關閉 ServerSocket，使 accept() 正常結束。
                }
            }
        }

        /**
         * 每個 SMTP 協定階段皆延遲回覆：委派工廠對非真 TLS 端點會先嘗試 SSL handshake 失敗
         * 後才退回明文 socket，同一次邏輯上的發送嘗試因而可能對應多個實體連線，故不再區分
         * 「第一個連線較快」，一律延遲以穩定驗證整體逾時上限（累加多階段延遲仍會被整體逾時截斷）。
         */
        private void handle(Socket socket) {
            try (socket;
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII))) {
                reply(writer, "220 localhost ESMTP ready");
                boolean readingData = false;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (readingData) {
                        if (".".equals(line)) {
                            acceptedMessages.incrementAndGet();
                            delayedReply(writer, "250 queued", true);
                            readingData = false;
                        }
                        continue;
                    }

                    String command = line.toUpperCase(Locale.ROOT);
                    commands.add(verbOf(command));
                    if (command.startsWith("EHLO") || command.startsWith("HELO")) {
                        reply(writer, "250-localhost\r\n250 8BITMIME");
                    } else if (command.startsWith("MAIL FROM") || command.startsWith("RCPT TO")) {
                        delayedReply(writer, "250 OK", true);
                    } else if (command.equals("DATA")) {
                        delayedReply(writer, "354 End data with <CR><LF>.<CR><LF>", true);
                        readingData = true;
                    } else if (command.equals("QUIT")) {
                        reply(writer, "221 Bye");
                        return;
                    } else {
                        reply(writer, "250 OK");
                    }
                }
            } catch (IOException ignored) {
                // 整體截止到達時客戶端會取消寄送；測試結束也會主動關閉 socket。
            } finally {
                clients.remove(socket);
            }
        }

        private void delayedReply(BufferedWriter writer, String response, boolean delayed) throws IOException {
            if (delayed) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            reply(writer, response);
        }

        private static void reply(BufferedWriter writer, String response) throws IOException {
            writer.write(response);
            writer.write("\r\n");
            writer.flush();
        }

        private boolean awaitNoClients(long timeoutMs) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            while (!clients.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            return clients.isEmpty();
        }

        private boolean awaitConnections(int expected, long timeoutMs) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            while (connectionCount.get() < expected && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            return connectionCount.get() >= expected;
        }

        /** 只保留 SMTP 指令動詞；TLS 探測送入的二進位資料一律記為 OTHER。 */
        private static String verbOf(String command) {
            for (String verb : List.of("EHLO", "HELO", "MAIL", "RCPT", "DATA", "QUIT")) {
                if (command.startsWith(verb)) {
                    return verb;
                }
            }
            return "OTHER";
        }

        /** 依收到順序記錄的 SMTP 指令動詞（不含 DATA 內容行）。 */
        private List<String> commands() {
            return List.copyOf(commands);
        }

        private int acceptedMessages() {
            return acceptedMessages.get();
        }

        private int connectionCount() {
            return connectionCount.get();
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            for (Socket client : clients) {
                client.close();
            }
        }
    }
}
