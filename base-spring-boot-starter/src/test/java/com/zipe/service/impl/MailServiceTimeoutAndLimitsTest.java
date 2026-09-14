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
    private MultiStageDelaySmtpServer delayedSmtp;

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void closeBlackHole() throws IOException {
        if (blackHole != null && !blackHole.isClosed()) {
            blackHole.close();
        }
        if (delayedSmtp != null) {
            delayedSmtp.close();
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

    /** SC-18：單次 SMTP 對話包含多個各自未逾時的延遲階段時，累積時間仍不得超過整體上限。 */
    @Test
    void overallTimeout_boundsWholeOperation_acrossMultipleSmtpStages() throws Exception {
        delayedSmtp = new MultiStageDelaySmtpServer(300);
        MailServerProperty server = new MailServerProperty();
        server.setName("multi-stage-delay");
        server.setHost("127.0.0.1");
        server.setPort(String.valueOf(delayedSmtp.port()));
        server.setSmtpAuthEnable(false);

        MailPropertyConfig cfg = config(server);
        cfg.setReadTimeout(2000);
        cfg.setConnectionTimeout(2000);
        cfg.setWriteTimeout(2000);
        cfg.getFailover().setOverallTimeout(500L);

        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        long startedNanos = System.nanoTime();
        assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-18-multi-stage")))
                .isInstanceOf(MailFailoverException.class)
                .hasMessageContaining("multi-stage-delay");
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);

        // 每個回應只延遲 300ms，均低於個別 read timeout；若只限制各 socket 操作，累積會超過 1 秒。
        assertThat(elapsedMs).isLessThanOrEqualTo(1000);
        assertThat(delayedSmtp.awaitNoClients(800)).isTrue();
        assertThat(delayedSmtp.acceptedMessages()).isZero();
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

    /** SocketFactory 的所有連線建立入口都必須受到同一絕對截止時間保護。 */
    @Test
    void deadlineSocketFactory_guardsEveryConnectedSocketCreationVariant() throws Exception {
        Class<?> factoryType = Class.forName(MailServiceImpl.class.getName() + "$DeadlineSocketFactory");
        var constructor = factoryType.getDeclaredConstructor(long.class);
        constructor.setAccessible(true);

        List<Socket> clients = new ArrayList<>();
        List<Socket> peers = new ArrayList<>();
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (ServerSocket listener = new ServerSocket(0, 50, loopback)) {
            SocketFactory factory =
                    (SocketFactory) constructor.newInstance(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500));

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
        MailServiceImpl service = initializedService(config);
        List<?> candidates = (List<?>) ReflectionTestUtils.getField(service, "candidates");
        assertThat(candidates).isNotNull().hasSize(1);
        return (JavaMailSenderImpl) ReflectionTestUtils.getField(candidates.get(0), "sender");
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
                    boolean delayed = connectionCount.incrementAndGet() > 1;
                    Thread client = new Thread(() -> handle(socket, delayed), "multi-stage-smtp-client");
                    client.setDaemon(true);
                    client.start();
                } catch (IOException ignored) {
                    // close() 會關閉 ServerSocket，使 accept() 正常結束。
                }
            }
        }

        private void handle(Socket socket, boolean delayed) {
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
                            delayedReply(writer, "250 queued", delayed);
                            readingData = false;
                        }
                        continue;
                    }

                    String command = line.toUpperCase(Locale.ROOT);
                    if (command.startsWith("EHLO") || command.startsWith("HELO")) {
                        reply(writer, "250-localhost\r\n250 8BITMIME");
                    } else if (command.startsWith("MAIL FROM") || command.startsWith("RCPT TO")) {
                        delayedReply(writer, "250 OK", delayed);
                    } else if (command.equals("DATA")) {
                        delayedReply(writer, "354 End data with <CR><LF>.<CR><LF>", delayed);
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

        private int acceptedMessages() {
            return acceptedMessages.get();
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
